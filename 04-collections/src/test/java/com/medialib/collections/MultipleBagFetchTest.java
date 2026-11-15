package com.medialib.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code MultipleBagFetchException} : l'erreur qu'on rencontre un jour, sans comprendre
 * pourquoi le code d'hier ne passe plus.
 *
 * <p>Joindre deux collections dans la meme requete produit un produit cartesien : 3 exemplaires
 * et 2 avis donnent 6 lignes. Pour un {@code Set}, Hibernate s'en sort — les doublons se
 * fondent. Pour un <em>bag</em>, qui accepte les doublons, il n'a aucun moyen de savoir
 * lesquelles des 6 lignes sont de vrais elements. Alors il refuse.
 */
class MultipleBagFetchTest extends CollectionsTestBase {

    private void unMediaAvecExemplairesEtAvis() {
        inTransaction(session -> {
            Media media = new Media("Une si longue lettre");
            media.addCopy(new Copy("EX-0001"));
            media.addCopy(new Copy("EX-0002"));
            media.addCopy(new Copy("EX-0003"));
            media.addReview(new Review(5, "magistral"));
            media.addReview(new Review(4, "tres bon"));
            session.persist(media);
        });
    }

    @Test
    @DisplayName("Deux bags en join fetch : Hibernate refuse de deviner")
    void deux_bags_en_join_fetch_levent_une_exception() {
        unMediaAvecExemplairesEtAvis();

        inTransaction(session -> assertThatThrownBy(() -> session.createQuery(
                "select m from Media m join fetch m.copies join fetch m.reviews", Media.class)
                .getResultList())
                .hasMessageContaining("MultipleBagFetch"));
    }

    @Test
    @DisplayName("Charger une collection a la fois : deux requetes, aucun probleme")
    void charger_les_deux_bags_separement_fonctionne() {
        unMediaAvecExemplairesEtAvis();

        inTransaction(session -> {
            sql().reset();

            Media media = session.createQuery(
                    "select m from Media m join fetch m.copies", Media.class).getSingleResult();
            media.getReviews().size(); // seconde requete, paresseuse

            assertThat(sql().selectCount()).isEqualTo(2);
            assertThat(media.getCopies()).hasSize(3);
            assertThat(media.getReviews()).hasSize(2);
        });
    }

    @Test
    @DisplayName("Depuis Hibernate 6, le distinct du join fetch est devenu inutile")
    void un_join_fetch_est_dedoublonne_automatiquement() {
        unMediaAvecExemplairesEtAvis();

        inTransaction(session -> {
            sql().reset();

            List<Media> resultats = session.createQuery(
                    "select m from Media m join fetch m.copies", Media.class).getResultList();

            // La jointure ramene bien trois lignes SQL pour trois exemplaires...
            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("join");

            // ...mais Hibernate 6+ replie les doublons tout seul. Pendant des annees, le
            // conseil etait d'ecrire "select distinct m" : c'est desormais inutile, et le
            // distinct ferait en plus trier la base pour rien.
            assertThat(resultats).hasSize(1);
            assertThat(resultats.get(0).getCopies()).hasSize(3);
        });
    }

    @Test
    @DisplayName("Le distinct explicite donne le meme resultat, en ajoutant un DISTINCT au SQL")
    void le_distinct_explicite_ne_change_que_le_sql() {
        unMediaAvecExemplairesEtAvis();

        inTransaction(session -> {
            sql().reset();

            List<Media> resultats = session.createQuery(
                    "select distinct m from Media m join fetch m.copies", Media.class)
                    .getResultList();

            assertThat(resultats).hasSize(1); // identique au test precedent
            assertThat(sql().lastQuery()).contains("distinct"); // mais la base travaille de plus
        });
    }
}
