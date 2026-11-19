package com.medialib.fetching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code @Fetch(SUBSELECT)} : la troisieme reponse au N+1 sur une collection.
 *
 * <p>Les deux premieres sont ailleurs dans le chapitre — {@code join fetch} ramene tout d'un
 * coup, {@code @BatchSize} regroupe par paquets d'identifiants. Celle-ci fait autre chose :
 * elle <strong>rejoue la requete d'origine</strong> comme sous-requete, pour charger d'un seul
 * coup les collections de <em>toutes</em> les entites qu'elle avait ramenees.
 *
 * <p>Son interet est qu'elle ne depend d'aucune liste d'identifiants, donc d'aucune taille de
 * paquet a regler. Son cout est qu'elle rejoue la requete d'origine, filtres et jointures
 * compris.
 */
class FetchSubselectTest extends ChargementTestBase {

    private static final int NOMBRE_DE_SERIES = 20;

    private void vingtSeriesDeTroisEpisodes() {
        inTransaction(session -> {
            for (int i = 1; i <= NOMBRE_DE_SERIES; i++) {
                Series series = new Series("Serie " + i);
                for (int e = 1; e <= 3; e++) {
                    series.addEpisode(new Episode("Episode " + e));
                }
                session.persist(series);
            }
        });
    }

    @Test
    @DisplayName("Une seule requete supplementaire pour les vingt collections, pas vingt")
    void subselect_charge_toutes_les_collections_en_une_requete() {
        vingtSeriesDeTroisEpisodes();

        inTransaction(session -> {
            sql().reset();

            List<Series> series = session
                    .createQuery("from Series s order by s.id", Series.class)
                    .getResultList();
            assertThat(series).hasSize(NOMBRE_DE_SERIES);
            assertThat(sql().selectCount()).isEqualTo(1);

            // On ne touche qu'a la collection de la PREMIERE serie...
            assertThat(series.get(0).getEpisodes()).hasSize(3);

            // ... et les vingt sont desormais chargees. Une requete, pas vingt.
            assertThat(sql().selectCount()).isEqualTo(2);
            for (Series s : series) {
                assertThat(s.getEpisodes()).hasSize(3);
            }
            assertThat(sql().selectCount()).isEqualTo(2);
        });
    }

    @Test
    @DisplayName("Le cout : la requete d'origine est rejouee a l'interieur de la seconde")
    void subselect_rejoue_la_requete_d_origine() {
        vingtSeriesDeTroisEpisodes();

        inTransaction(session -> {
            List<Series> series = session
                    .createQuery("from Series s where s.title like 'Serie 1%' order by s.id",
                            Series.class)
                    .getResultList();

            sql().reset();
            series.get(0).getEpisodes().size();

            // La seconde requete ne transporte aucun identifiant : elle re-selectionne les
            // series par le MEME critere, en sous-requete. C'est ce qui la dispense d'une
            // liste d'identifiants — et ce qui la rend chere si la requete d'origine etait
            // elle-meme couteuse, puisqu'on la paie une seconde fois.
            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery())
                    // La requete d'origine, filtre compris, est litteralement reimbriquee.
                    .contains("where e1_0.series_id in (select s1_0.id from series s1_0 "
                            + "where s1_0.title like 'serie 1%'")
                    // Aucun identifiant n'est transporte : ni liste in (?, ?, ...), ni la
                    // signature `= any(?)` que @BatchSize produit sur PostgreSQL.
                    .doesNotContain("= any(?)");
        });
    }

    @Test
    @DisplayName("Hors requete, le subselect n'a rien a rejouer : on retombe sur un N+1")
    void subselect_ne_sert_a_rien_sur_des_entites_chargees_par_find() {
        vingtSeriesDeTroisEpisodes();

        List<Long> ids = inTransactionReturning(session -> session
                .createQuery("select s.id from Series s order by s.id", Long.class)
                .setMaxResults(3)
                .getResultList());

        inTransaction(session -> {
            List<Series> series = ids.stream().map(id -> session.find(Series.class, id)).toList();
            sql().reset();

            for (Series s : series) {
                s.getEpisodes().size();
            }

            // Trois collections, trois requetes. La strategie n'a aucune requete d'origine a
            // rejouer : elle n'a de sens que pour des entites ramenees par une requete. C'est
            // sa limite face a @BatchSize, qui regroupe quelle que soit leur provenance.
            assertThat(sql().selectCount()).isEqualTo(3);
        });
    }
}
