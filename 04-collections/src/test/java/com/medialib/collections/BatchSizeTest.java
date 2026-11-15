package com.medialib.collections;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code @BatchSize} : la solution la moins connue au N+1, et souvent la plus simple.
 *
 * <p>Au lieu d'une requete par collection, Hibernate regroupe les cles en attente et les
 * charge par paquets. Sur PostgreSQL, cela donne {@code where media_id = any (?)}.
 */
class BatchSizeTest extends CollectionsTestBase {

    private void dixOeuvresAvecAvisEtExemplaires() {
        inTransaction(session -> {
            for (int i = 1; i <= 10; i++) {
                Media media = new Media("Oeuvre " + i);
                media.addCopy(new Copy("EX-" + i));
                media.addReview(new Review(4, "avis sur l'oeuvre " + i));
                session.persist(media);
            }
        });
    }

    @Test
    @DisplayName("Sans @BatchSize : une requete par collection, le N+1 classique")
    void sans_batch_size_chaque_collection_coute_une_requete() {
        dixOeuvresAvecAvisEtExemplaires();

        inTransaction(session -> {
            List<Media> medias =
                    session.createQuery("select m from Media m", Media.class).getResultList();
            sql().reset();

            medias.forEach(m -> m.getCopies().size()); // copies n'a pas de @BatchSize

            assertThat(sql().selectCount()).isEqualTo(10); // une par oeuvre
        });
    }

    @Test
    @DisplayName("Avec @BatchSize(5) : deux requetes au lieu de dix")
    void batchsize_regroupe_le_chargement_des_collections() {
        dixOeuvresAvecAvisEtExemplaires();

        inTransaction(session -> {
            List<Media> medias =
                    session.createQuery("select m from Media m", Media.class).getResultList();
            sql().reset();

            medias.forEach(m -> m.getReviews().size()); // reviews porte @BatchSize(size = 5)

            assertThat(sql().selectCount()).isEqualTo(2); // 10 / 5

            // Sur PostgreSQL, Hibernate 6+ n'ecrit pas "in (?, ?, ?, ?, ?)" mais passe un
            // tableau : "= any (?)". Un seul parametre au lieu de cinq, donc un seul plan
            // d'execution en cache quel que soit le nombre de cles.
            assertThat(sql().lastQuery()).contains("= any (?)");
        });
    }
}
