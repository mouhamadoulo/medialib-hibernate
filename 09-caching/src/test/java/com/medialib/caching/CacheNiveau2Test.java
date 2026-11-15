package com.medialib.caching;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le cache de second niveau : partage entre toutes les sessions d'une meme fabrique.
 *
 * <p>C'est le seul cache qui survit a la fin d'une session, donc le seul qui epargne
 * reellement des requetes a une application qui sert plusieurs utilisateurs.
 */
class CacheNiveau2Test extends CacheTestBase {

    /** Vide le cache L2 pour partir d'un cache froid, comme apres un redemarrage. */
    private void cacheFroid() {
        sessionFactory().getCache().evictAllRegions();
    }

    @Test
    @DisplayName("L'INSERT peuple deja le cache : la premiere lecture n'emet aucune requete")
    void une_insertion_place_l_entite_dans_le_cache() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("Une si longue lettre");
            session.persist(media);
            return media.getId();
        });

        // Surprise fréquente : on n'a encore rien lu, et pourtant le cache repond deja.
        // En READ_WRITE, Hibernate place l'entite en cache au moment de l'insertion — il
        // connait son etat exact, autant en profiter.
        inTransaction(session -> {
            sql().reset();
            assertThat(session.find(Media.class, id).getTitle())
                    .isEqualTo("Une si longue lettre");
            assertThat(sql().selectCount()).isZero();
        });
    }

    @Test
    @DisplayName("Cache froid : la premiere lecture requete, la seconde non")
    void une_entite_cachee_n_est_lue_qu_une_fois_entre_deux_sessions() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("Une si longue lettre");
            session.persist(media);
            return media.getId();
        });
        cacheFroid();

        // Premiere session apres un cache vide : il faut aller en base.
        inTransaction(session -> {
            sql().reset();
            assertThat(session.find(Media.class, id).getTitle())
                    .isEqualTo("Une si longue lettre");
            assertThat(sql().selectCount()).isEqualTo(1);
        });

        // Seconde session : le cache L2 repond, aucune requete.
        inTransaction(session -> {
            sql().reset();
            assertThat(session.find(Media.class, id).getTitle())
                    .isEqualTo("Une si longue lettre");
            assertThat(sql().selectCount()).isZero();
        });
    }

    @Test
    @DisplayName("Les statistiques remontent le miss puis le hit")
    void les_statistiques_remontent_hit_et_miss() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("L'Aventure ambigue");
            session.persist(media);
            return media.getId();
        });
        cacheFroid();
        videLesStatistiques();

        inTransaction(session -> session.find(Media.class, id));
        assertThat(stats().getSecondLevelCacheMissCount()).isEqualTo(1);
        assertThat(stats().getSecondLevelCacheHitCount()).isZero();

        inTransaction(session -> session.find(Media.class, id));
        assertThat(stats().getSecondLevelCacheHitCount()).isEqualTo(1);
        assertThat(stats().getSecondLevelCacheMissCount()).isEqualTo(1); // toujours un seul
    }

    @Test
    @DisplayName("Une entite non annotee n'entre jamais dans le cache L2")
    void une_entite_non_annotee_n_est_pas_cachee() {
        Long id = inTransactionReturning(session -> {
            Loan loan = new Loan(java.time.LocalDate.of(2026, 3, 1));
            session.persist(loan);
            return loan.getId();
        });

        inTransaction(session -> session.find(Loan.class, id));

        inTransaction(session -> {
            sql().reset();
            session.find(Loan.class, id);
            assertThat(sql().selectCount()).isEqualTo(1); // requete a chaque session
        });
    }

    @Test
    @DisplayName("Une modification met le cache a jour : la session suivante ne requete pas")
    void read_write_met_le_cache_a_jour_au_commit() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("Les Bouts de bois de Dieu");
            session.persist(media);
            return media.getId();
        });

        inTransaction(session -> session.find(Media.class, id).setTitle("Titre corrige"));

        inTransaction(session -> {
            sql().reset();
            Media relu = session.find(Media.class, id);

            // READ_WRITE ne se contente pas d'invalider : au commit, il remplace l'entree
            // par l'etat valide. La lecture suivante est donc servie par le cache, a jour.
            assertThat(relu.getTitle()).isEqualTo("Titre corrige");
            assertThat(sql().selectCount()).isZero();
        });
    }
}
