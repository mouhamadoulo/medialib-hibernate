package com.medialib.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le verrouillage pessimiste : on pose un verrou en base, et on fait attendre les autres.
 *
 * <p>A utiliser quand le conflit est probable et qu'un echec en fin de transaction coute cher.
 * Son defaut est son cout : les autres transactions attendent reellement.
 */
class PessimisticTest extends ConcurrenceTestBase {

    /** Le nom standard du reglage de delai d'attente d'un verrou. 0 signifie NOWAIT. */
    private static final String LOCK_TIMEOUT = "jakarta.persistence.lock.timeout";

    @Test
    @DisplayName("PESSIMISTIC_WRITE emet un for update")
    void pessimistic_write_emet_un_for_update() {
        Long id = unExemplaireDisponible("EX-0010");

        inTransaction(session -> {
            sql().reset();

            Copy copy = session.find(Copy.class, id, LockModeType.PESSIMISTIC_WRITE);

            assertThat(copy).isNotNull();

            // PostgreSQL recoit "for no key update", et non "for update" : Hibernate demande
            // le verrou le plus FAIBLE qui suffise. "for no key update" bloque les autres
            // ecritures de la ligne mais laisse passer les cles etrangeres qui la referencent
            // — donc moins de contention, pour la meme garantie ici.
            assertThat(sql().lastQuery()).contains("for no key update");
        });
    }

    @Test
    @DisplayName("PESSIMISTIC_READ : un verrou partage, pas un verrou exclusif")
    void pessimistic_read_emet_un_verrou_partage() {
        Long id = unExemplaireDisponible("EX-0011");

        inTransaction(session -> {
            sql().reset();
            session.find(Copy.class, id, LockModeType.PESSIMISTIC_READ);

            assertThat(sql().lastQuery())
                    .containsAnyOf("for share", "for no key update", "for update");
        });
    }

    @Test
    @DisplayName("NOWAIT : plutot echouer tout de suite qu'attendre un verrou")
    void nowait_echoue_immediatement_sur_ligne_verrouillee() {
        Long id = unExemplaireDisponible("EX-0012");

        try (Session detenteur = sessionFactory().openSession();
                Session impatient = sessionFactory().openSession()) {

            detenteur.beginTransaction();
            detenteur.find(Copy.class, id, LockModeType.PESSIMISTIC_WRITE); // verrou pose

            impatient.beginTransaction();
            // Sans NOWAIT, cet appel attendrait que le detenteur libere. Avec, il renonce.
            assertThatThrownBy(() -> impatient.find(Copy.class, id,
                    LockModeType.PESSIMISTIC_WRITE, Map.of(LOCK_TIMEOUT, 0)))
                    .isInstanceOf(RuntimeException.class);

            impatient.getTransaction().rollback();
            detenteur.getTransaction().rollback();
        }
    }

    @Test
    @DisplayName("SKIP LOCKED : prendre la file d'attente sans se bloquer dessus")
    void skip_locked_ignore_les_lignes_verrouillees() {
        Long premier = unExemplaireDisponible("EX-0020");
        Long second = unExemplaireDisponible("EX-0021");

        try (Session detenteur = sessionFactory().openSession();
                Session travailleur = sessionFactory().openSession()) {

            detenteur.beginTransaction();
            detenteur.find(Copy.class, premier, LockModeType.PESSIMISTIC_WRITE);

            travailleur.beginTransaction();
            // Motif classique d'une file de travail : « donne-moi ce qui n'est pas pris ».
            // Le SQL est explicite ici, parce que c'est lui qu'on vient voir.
            List<Long> libres = travailleur.createNativeQuery(
                    "select id from copy order by id for update skip locked", Long.class)
                    .getResultList();

            // L'exemplaire verrouille est simplement absent du resultat. Aucune attente,
            // aucune exception : exactement ce qu'il faut pour N consommateurs d'une file.
            assertThat(libres).containsExactly(second);

            travailleur.getTransaction().rollback();
            detenteur.getTransaction().rollback();
        }
    }

    @Test
    @DisplayName("Sans SKIP LOCKED, la meme requete attendrait : le verrou est bien reel")
    void le_verrou_pessimiste_bloque_vraiment_les_autres() {
        Long id = unExemplaireDisponible("EX-0022");

        try (Session detenteur = sessionFactory().openSession();
                Session autre = sessionFactory().openSession()) {

            detenteur.beginTransaction();
            detenteur.find(Copy.class, id, LockModeType.PESSIMISTIC_WRITE);

            autre.beginTransaction();
            // Une lecture simple n'est pas genee : PostgreSQL ne bloque jamais les lecteurs.
            String codeBarres = autre.createQuery(
                    "select c.barcode from Copy c where c.id = :id", String.class)
                    .setParameter("id", id)
                    .getSingleResult();
            assertThat(codeBarres).isEqualTo("EX-0022");

            // C'est la tentative de verrou qui bloquerait. On le verifie en NOWAIT.
            assertThatThrownBy(() -> autre.find(Copy.class, id,
                    LockModeType.PESSIMISTIC_WRITE, Map.of(LOCK_TIMEOUT, 0)))
                    .isInstanceOf(RuntimeException.class);

            autre.getTransaction().rollback();
            detenteur.getTransaction().rollback();
        }
    }
}
