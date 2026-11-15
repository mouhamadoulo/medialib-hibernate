package com.medialib.performance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.hibernate.StatelessSession;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les trois facons de traiter du volume sans faire exploser la memoire ni le nombre de
 * requetes : {@link StatelessSession}, les operations en masse, et le streaming.
 */
class StatelessEtBulkTest extends PerfTestBase {

    @Override
    protected Map<String, Object> reglages() {
        return Map.of(AvailableSettings.STATEMENT_BATCH_SIZE, TAILLE_DE_LOT);
    }

    private void centOeuvres() {
        inTransaction(session -> {
            for (int i = 1; i <= 100; i++) {
                session.persist(new Media("Oeuvre " + i));
            }
        });
    }

    @Test
    @DisplayName("StatelessSession : ni cache L1, ni dirty checking, ni cascade")
    void une_stateless_session_n_a_ni_cache_l1_ni_dirty_checking() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("Une si longue lettre");
            session.persist(media);
            return media.getId();
        });

        try (StatelessSession stateless = sessionFactory().openStatelessSession()) {
            var transaction = stateless.beginTransaction();

            Media media = stateless.get(Media.class, id);
            jdbc().reset();

            media.setTitle("Modifie sans effet"); // personne ne surveille

            transaction.commit();
            assertThat(jdbc().executions()).isZero(); // aucune ecriture
        }

        // La base n'a rien vu.
        String titre = inTransactionReturning(session -> session.find(Media.class, id).getTitle());
        assertThat(titre).isEqualTo("Une si longue lettre");

        // Pour ecrire avec une StatelessSession, il faut le demander explicitement.
        try (StatelessSession stateless = sessionFactory().openStatelessSession()) {
            var transaction = stateless.beginTransaction();
            Media media = stateless.get(Media.class, id);
            media.setTitle("Modifie explicitement");
            stateless.update(media);
            transaction.commit();
        }

        String apres = inTransactionReturning(session -> session.find(Media.class, id).getTitle());
        assertThat(apres).isEqualTo("Modifie explicitement");
    }

    @Test
    @DisplayName("StatelessSession : deux lectures du meme identifiant rendent deux instances")
    void une_stateless_session_ne_garantit_aucune_identite() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("L'Aventure ambigue");
            session.persist(media);
            return media.getId();
        });

        try (StatelessSession stateless = sessionFactory().openStatelessSession()) {
            var transaction = stateless.beginTransaction();

            Media premier = stateless.get(Media.class, id);
            Media second = stateless.get(Media.class, id);

            // Sans contexte de persistance, chaque lecture est une lecture.
            assertThat(second).isNotSameAs(premier);
            transaction.commit();
        }
    }

    @Test
    @DisplayName("Un UPDATE en masse : une seule instruction pour cent lignes")
    void un_update_hql_en_masse_ne_fait_qu_une_requete() {
        centOeuvres();

        inTransaction(session -> {
            jdbc().reset();

            int touchees = session
                    .createMutationQuery("update Media m set m.loanCount = m.loanCount + 1")
                    .executeUpdate();

            assertThat(touchees).isEqualTo(100);
            assertThat(jdbc().executionsDe("update")).isEqualTo(1); // une instruction, cent lignes
        });
    }

    @Test
    @DisplayName("Le piege du bulk : le contexte de persistance devient perime")
    void le_contexte_de_persistance_devient_perime_apres_un_bulk() {
        centOeuvres();

        inTransaction(session -> {
            Media media = session.createQuery("select m from Media m order by m.id", Media.class)
                    .setMaxResults(1).getSingleResult();
            assertThat(media.getLoanCount()).isZero();

            session.createMutationQuery("update Media m set m.loanCount = 42").executeUpdate();

            // L'instance en session n'a pas bouge : le bulk update parle a la base, pas au
            // contexte de persistance. C'est le piege, et il est silencieux.
            assertThat(media.getLoanCount()).isZero();

            session.refresh(media);
            assertThat(media.getLoanCount()).isEqualTo(42);
        });
    }

    @Test
    @DisplayName("Un DELETE en masse, de meme : une instruction")
    void un_delete_hql_en_masse_ne_fait_qu_une_requete() {
        centOeuvres();

        inTransaction(session -> {
            jdbc().reset();
            int supprimees = session.createMutationQuery("delete from Media").executeUpdate();

            assertThat(supprimees).isEqualTo(100);
            assertThat(jdbc().executionsDe("delete")).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("stream : parcourir sans tout charger, a condition de vider le contexte")
    void stream_ne_charge_pas_tout_en_memoire() {
        centOeuvres();

        inTransaction(session -> {
            session.clear();
            jdbc().reset();

            List<String> titres;
            try (var flux = session.createQuery("select m from Media m", Media.class)
                    .setFetchSize(10)
                    .getResultStream()) {
                titres = flux.map(Media::getTitle).toList();
            }

            assertThat(titres).hasSize(100);
            // Une seule requete, parcourue au fil de l'eau.
            assertThat(jdbc().executionsDe("select")).isEqualTo(1);

            // Attention : les entites restent dans le contexte de persistance. Sur un vrai
            // gros volume, il faut appeler session.clear() periodiquement, sinon le streaming
            // economise le reseau mais remplit la memoire — ce qui annule tout le benefice.
            assertThat(session.getStatistics().getEntityCount()).isEqualTo(100);
        });
    }
}
