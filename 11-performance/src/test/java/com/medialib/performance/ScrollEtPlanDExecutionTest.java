package com.medialib.performance;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.ScrollableResults;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lire gros sans tout charger, et verifier ce que la base fait vraiment de nos requetes.
 *
 * <p>Deux outils que le chapitre annonce et qui se repondent : {@code scroll()} borne la
 * memoire de l'application, {@code explain analyze} dit si la requete, elle, est seulement
 * raisonnable. L'un ne dispense pas de l'autre — un curseur sur un parcours sequentiel de
 * table reste un parcours sequentiel de table.
 */
class ScrollEtPlanDExecutionTest extends PerfTestBase {

    private static final int NOMBRE_D_OEUVRES = 5_000;

    private void cinqMilleOeuvres() {
        inTransaction(session -> {
            for (int i = 1; i <= NOMBRE_D_OEUVRES; i++) {
                session.persist(new Media("Oeuvre " + i));
                if (i % 500 == 0) {
                    session.flush();
                    session.clear();
                }
            }
        });
    }

    @Test
    @DisplayName("scroll() ouvre un curseur : une seule execution JDBC pour cinq mille lignes")
    void scroll_parcourt_en_une_seule_execution() {
        cinqMilleOeuvres();

        inTransaction(session -> {
            jdbc().reset();

            int lues = 0;
            try (ScrollableResults<Media> curseur = session
                    .createSelectionQuery("from Media m order by m.id", Media.class)
                    .setFetchSize(100)
                    .scroll()) {

                while (curseur.next()) {
                    curseur.get();
                    lues++;
                }
            }

            assertThat(lues).isEqualTo(NOMBRE_D_OEUVRES);

            // Une execution, pas cinq mille : le pilote ramene les lignes par paquets de
            // fetchSize sur un curseur ouvert, sans que l'application n'emette d'autre ordre.
            assertThat(jdbc().executionsDe("select")).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("Le piege : le curseur borne le reseau, pas le contexte de persistance")
    void scroll_sans_clear_accumule_tout_en_memoire() {
        cinqMilleOeuvres();

        inTransaction(session -> {
            try (ScrollableResults<Media> curseur = session
                    .createSelectionQuery("from Media m order by m.id", Media.class)
                    .setFetchSize(100)
                    .scroll()) {

                while (curseur.next()) {
                    curseur.get();
                }
            }

            // Toutes les entites parcourues sont restees managees. On a donc « streame » le
            // reseau et charge l'intégralite de la table en memoire — l'erreur exacte que
            // scroll() est censee eviter, et elle ne se voit sur aucun compteur de requetes.
            assertThat(session.getStatistics().getEntityCount()).isEqualTo(NOMBRE_D_OEUVRES);
        });

        inTransaction(session -> {
            int lues = 0;
            try (ScrollableResults<Media> curseur = session
                    .createSelectionQuery("from Media m order by m.id", Media.class)
                    .setFetchSize(100)
                    .scroll()) {

                while (curseur.next()) {
                    curseur.get();
                    lues++;
                    if (lues % 100 == 0) {
                        session.clear(); // la ligne qui manquait
                    }
                }
            }

            // Avec le clear periodique, le contexte reste borne quelle que soit la volumetrie.
            // C'est cette ligne, et non scroll() seul, qui rend le parcours constant en
            // memoire.
            assertThat(session.getStatistics().getEntityCount()).isLessThan(200);
        });
    }

    @Test
    @DisplayName("explain analyze : sans index declare, la recherche par titre balaie la table")
    void le_mapping_ne_cree_aucun_index_sur_les_colonnes_de_recherche() {
        cinqMilleOeuvres();
        supprimeLIndexDeTitre();

        String planSansIndex = planDe("select m.id, m.title from media m where m.title = 'Oeuvre 4242'");

        // hbm2ddl ne cree que ce que le mapping declare : cles primaires, cles etrangeres,
        // contraintes d'unicite. Une colonne sur laquelle l'application cherche en permanence
        // n'a aucun index, et rien dans le mapping ne le signale.
        assertThat(planSansIndex).contains("Seq Scan on media");

        inTransaction(session -> {
            session.createNativeMutationQuery(
                    "create index idx_media_title on media (title)").executeUpdate();
            session.createNativeMutationQuery("analyze media").executeUpdate();
        });

        String planAvecIndex = planDe("select m.id, m.title from media m where m.title = 'Oeuvre 4242'");

        // Le mapping n'a pas change d'un caractere : seul le schema l'a fait. C'est la raison
        // pour laquelle ce chapitre est aussi celui de Flyway — un index est une decision de
        // schema, qui se versionne, et que `@Table(indexes = ...)` ne suffit pas a piloter
        // des que le schema cesse d'etre genere.
        assertThat(planAvecIndex).contains("Index Scan").doesNotContain("Seq Scan on media");

        supprimeLIndexDeTitre();
    }

    private void supprimeLIndexDeTitre() {
        inTransaction(session -> session
                .createNativeMutationQuery("drop index if exists idx_media_title").executeUpdate());
    }

    /** Le plan reellement choisi par PostgreSQL, lignes concatenees. */
    private String planDe(String sql) {
        return inTransactionReturning(session -> String.join("\n", session
                .createNativeQuery("explain analyze " + sql, String.class)
                .getResultList()));
    }
}
