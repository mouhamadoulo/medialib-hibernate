package com.medialib.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le scenario qui justifie tout ce chapitre.
 *
 * <p>Il reste un exemplaire de « Une si longue lettre ». Deux adherents cliquent « emprunter »
 * a la meme seconde. Un seul doit l'obtenir, et l'autre doit l'apprendre — pas repartir en
 * croyant avoir reussi.
 *
 * <p>Les deux threads sont synchronises par une {@link CyclicBarrier} : ils lisent tous les
 * deux l'exemplaire avant que l'un ou l'autre n'ecrive. Sans cette barriere, le test passerait
 * par hasard la plupart du temps, et c'est exactement ce qui rend ces bugs si difficiles a
 * reproduire en production.
 */
class EmpruntConcurrentTest extends ConcurrenceTestBase {

    @Test
    @DisplayName("Deux adherents sur le dernier exemplaire : exactement un gagne")
    void deux_adherents_sur_le_dernier_exemplaire_un_seul_gagne() throws Exception {
        Long copyId = unExemplaireDisponible("EX-DERNIER");
        Long awa = unAdherent("Awa Diop");
        Long ibrahima = unAdherent("Ibrahima Fall");

        CyclicBarrier lesDeuxOntLu = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> tentativeAwa = pool.submit(emprunt(copyId, awa, lesDeuxOntLu));
            Future<Boolean> tentativeIbrahima = pool.submit(emprunt(copyId, ibrahima, lesDeuxOntLu));

            boolean awaAEu = tentativeAwa.get(30, TimeUnit.SECONDS);
            boolean ibrahimaAEu = tentativeIbrahima.get(30, TimeUnit.SECONDS);

            // Un seul, jamais zero, jamais deux.
            assertThat(awaAEu ^ ibrahimaAEu).isTrue();
        } finally {
            pool.shutdownNow();
        }

        // Et la base le confirme : un seul emprunt, l'exemplaire est sorti.
        inTransaction(session -> {
            List<Loan> emprunts =
                    session.createQuery("select l from Loan l", Loan.class).getResultList();
            assertThat(emprunts).hasSize(1);
            assertThat(session.find(Copy.class, copyId).isAvailable()).isFalse();
        });
    }

    /**
     * Une tentative d'emprunt, en verrouillage <strong>optimiste</strong>.
     *
     * @return {@code true} si cet adherent a obtenu l'exemplaire
     */
    private Callable<Boolean> emprunt(Long copyId, Long memberId, CyclicBarrier barriere) {
        return () -> {
            try (Session session = sessionFactory().openSession()) {
                session.beginTransaction();

                Copy copy = session.find(Copy.class, copyId);
                Member member = session.find(Member.class, memberId);

                barriere.await(30, TimeUnit.SECONDS); // les deux ont lu la version 0

                if (!copy.isAvailable()) {
                    session.getTransaction().rollback();
                    return false;
                }
                copy.preter();
                session.persist(new Loan(member, copy, LocalDate.of(2026, 3, 1)));

                session.getTransaction().commit();
                return true;
            } catch (RuntimeException conflit) {
                // OptimisticLockException : l'autre a ecrit entre notre lecture et notre commit.
                return false;
            }
        };
    }

    @Test
    @DisplayName("En verrouillage pessimiste, le perdant attend puis constate, au lieu d'echouer")
    void en_pessimiste_le_second_attend_son_tour_et_voit_la_realite() throws Exception {
        Long copyId = unExemplaireDisponible("EX-DERNIER-2");
        Long awa = unAdherent("Awa Diop");
        Long ibrahima = unAdherent("Ibrahima Fall");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> premier = pool.submit(empruntPessimiste(copyId, awa));
            Future<Boolean> second = pool.submit(empruntPessimiste(copyId, ibrahima));

            boolean a = premier.get(30, TimeUnit.SECONDS);
            boolean b = second.get(30, TimeUnit.SECONDS);

            assertThat(a ^ b).isTrue();
        } finally {
            pool.shutdownNow();
        }

        Long emprunts = inTransactionReturning(session ->
                session.createQuery("select count(l) from Loan l", Long.class).getSingleResult());
        assertThat(emprunts).isEqualTo(1L);
    }

    /**
     * La meme tentative, en verrouillage <strong>pessimiste</strong>.
     *
     * <p>Difference essentielle : le {@code for update} est pose avant la lecture de la
     * disponibilite. Le second thread attend que le premier ait fini, puis lit un exemplaire
     * deja sorti et renonce proprement. Aucune exception, aucune relance a ecrire.
     */
    private Callable<Boolean> empruntPessimiste(Long copyId, Long memberId) {
        return () -> {
            try (Session session = sessionFactory().openSession()) {
                session.beginTransaction();

                Copy copy = session.find(Copy.class, copyId, LockModeType.PESSIMISTIC_WRITE);
                if (!copy.isAvailable()) {
                    session.getTransaction().rollback();
                    return false;
                }
                Member member = session.find(Member.class, memberId);
                copy.preter();
                session.persist(new Loan(member, copy, LocalDate.of(2026, 3, 1)));

                session.getTransaction().commit();
                return true;
            }
        };
    }
}
