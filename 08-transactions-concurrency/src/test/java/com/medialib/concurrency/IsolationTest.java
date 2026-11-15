package com.medialib.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les anomalies que le niveau d'isolation autorise.
 *
 * <p>PostgreSQL est en {@code READ COMMITTED} par defaut : une transaction ne voit jamais de
 * donnees non validees, mais rien ne garantit que deux lectures successives rendent la meme
 * valeur. C'est la <em>lecture non repetable</em>.
 *
 * <p>Point important et souvent mal comprise : le cache de premier niveau d'Hibernate
 * <strong>masque</strong> cette anomalie pour les entites. Une seconde {@code find()} rend
 * l'instance deja en session, sans requeter. L'anomalie existe toujours en base — il faut
 * une projection scalaire ou un {@code refresh()} pour la voir.
 */
class IsolationTest extends ConcurrenceTestBase {

    @Test
    @DisplayName("Lecture non repetable, visible par une projection scalaire")
    void lecture_non_repetable_sous_read_committed() {
        Long id = unExemplaireDisponible("EX-0030");

        try (Session lectrice = sessionFactory().openSession()) {
            lectrice.beginTransaction();

            String premiereLecture = codeBarres(lectrice, id);
            assertThat(premiereLecture).isEqualTo("EX-0030");

            // Une autre transaction modifie et valide, pendant que la notre est ouverte.
            inTransaction(autre -> autre.find(Copy.class, id).setBarcode("EX-0030-modifie"));

            String secondeLecture = codeBarres(lectrice, id);

            assertThat(secondeLecture).isEqualTo("EX-0030-modifie"); // la meme requete, deux reponses
            lectrice.getTransaction().rollback();
        }
    }

    @Test
    @DisplayName("Le cache de premier niveau masque l'anomalie sur les entites")
    void le_cache_de_session_masque_la_lecture_non_repetable() {
        Long id = unExemplaireDisponible("EX-0031");

        try (Session lectrice = sessionFactory().openSession()) {
            lectrice.beginTransaction();

            Copy copy = lectrice.find(Copy.class, id);
            assertThat(copy.getBarcode()).isEqualTo("EX-0031");

            inTransaction(autre -> autre.find(Copy.class, id).setBarcode("EX-0031-modifie"));

            // Seconde lecture : aucune requete, l'instance vient du contexte de persistance.
            Copy relu = lectrice.find(Copy.class, id);
            assertThat(relu).isSameAs(copy);
            assertThat(relu.getBarcode()).isEqualTo("EX-0031"); // valeur perimee, et stable

            // refresh() force le retour en base.
            lectrice.refresh(relu);
            assertThat(relu.getBarcode()).isEqualTo("EX-0031-modifie");

            lectrice.getTransaction().rollback();
        }
    }

    @Test
    @DisplayName("Aucune transaction ne voit les donnees non validees d'une autre")
    void aucune_lecture_sale_n_est_possible() {
        Long id = unExemplaireDisponible("EX-0032");

        try (Session ecrivain = sessionFactory().openSession();
                Session lecteur = sessionFactory().openSession()) {

            ecrivain.beginTransaction();
            ecrivain.find(Copy.class, id).setBarcode("EX-0032-non-valide");
            ecrivain.flush(); // envoye a la base, mais pas valide

            lecteur.beginTransaction();
            assertThat(codeBarres(lecteur, id)).isEqualTo("EX-0032"); // l'ancienne valeur
            lecteur.getTransaction().rollback();

            ecrivain.getTransaction().rollback();
        }
    }

    /** Une projection scalaire : elle ne passe par aucun cache d'entites. */
    private static String codeBarres(Session session, Long id) {
        return session.createQuery("select c.barcode from Copy c where c.id = :id", String.class)
                .setParameter("id", id)
                .getSingleResult();
    }
}
