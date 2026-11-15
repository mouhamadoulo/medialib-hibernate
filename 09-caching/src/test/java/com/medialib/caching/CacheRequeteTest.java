package com.medialib.caching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le cache de requetes : celui qu'on active par reflexe, et qui deçoit presque toujours.
 *
 * <p>Il memorise les <strong>identifiants</strong> rendus par une requete, pas les entites.
 * Un hit ne dispense donc pas de charger les entites — il les prend dans le cache L2, s'il
 * y sont. Et toute ecriture sur une table interrogee invalide tout ce qui la concerne.
 */
class CacheRequeteTest extends CacheTestBase {

    private void troisOeuvres() {
        inTransaction(session -> {
            session.persist(new Media("Une si longue lettre"));
            session.persist(new Media("L'Aventure ambigue"));
            session.persist(new Media("Les Bouts de bois de Dieu"));
        });
    }

    @Test
    @DisplayName("Sans cacheable(true), la requete repart en base a chaque fois")
    void une_requete_non_cachee_est_rejouee() {
        troisOeuvres();

        for (int i = 0; i < 2; i++) {
            inTransaction(session -> {
                sql().reset();
                session.createQuery("select m from Media m", Media.class).getResultList();
                assertThat(sql().selectCount()).isEqualTo(1);
            });
        }
    }

    @Test
    @DisplayName("Avec setCacheable(true), la seconde execution n'emet aucune requete")
    void une_requete_cachee_n_est_executee_qu_une_fois() {
        troisOeuvres();
        videLesStatistiques();

        inTransaction(session -> {
            List<Media> premier = session.createQuery("select m from Media m", Media.class)
                    .setCacheable(true)
                    .getResultList();
            assertThat(premier).hasSize(3);
        });
        assertThat(stats().getQueryCacheMissCount()).isEqualTo(1);

        inTransaction(session -> {
            sql().reset();
            List<Media> second = session.createQuery("select m from Media m", Media.class)
                    .setCacheable(true)
                    .getResultList();

            assertThat(second).hasSize(3);
            // Les identifiants viennent du cache de requetes, les entites du cache L2.
            assertThat(sql().selectCount()).isZero();
        });
        assertThat(stats().getQueryCacheHitCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Toute ecriture sur la table invalide le cache de requetes")
    void une_insertion_invalide_le_cache_de_requetes() {
        troisOeuvres();

        inTransaction(session -> session.createQuery("select m from Media m", Media.class)
                .setCacheable(true).getResultList());

        // Une seule insertion, et tout le cache de requetes portant sur Media tombe.
        inTransaction(session -> session.persist(new Media("Une quatrieme oeuvre")));

        inTransaction(session -> {
            sql().reset();
            List<Media> apres = session.createQuery("select m from Media m", Media.class)
                    .setCacheable(true).getResultList();

            assertThat(apres).hasSize(4);
            assertThat(sql().selectCount()).isEqualTo(1); // rejouee

            // D'ou la conclusion du chapitre : sur une table qui bouge, le cache de requetes
            // ajoute du travail sans rien economiser. Il ne vaut que sur des donnees quasi
            // immuables — et dans ce cas, le cache d'entites suffit souvent.
        });
    }
}
