package com.medialib.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.LockModeType;
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
 * L'interblocage, provoque puis evite.
 *
 * <p>Deux transactions verrouillent les memes deux lignes, dans l'ordre inverse : chacune
 * attend ce que l'autre detient. PostgreSQL le detecte et tue l'une des deux.
 *
 * <p>Le remede ne vient d'aucune annotation : il suffit que tout le code verrouille
 * <strong>toujours dans le meme ordre</strong>. Ici, par identifiant croissant.
 */
class InterblocageTest extends ConcurrenceTestBase {

    @Test
    @DisplayName("Verrouiller en ordre inverse : PostgreSQL tue l'une des deux transactions")
    void l_ordre_inverse_provoque_un_interblocage() throws Exception {
        Long premier = unExemplaireDisponible("EX-0040");
        Long second = unExemplaireDisponible("EX-0041");

        CyclicBarrier chacunSonPremierVerrou = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> montant =
                    pool.submit(verrouille(List.of(premier, second), chacunSonPremierVerrou));
            Future<Boolean> descendant =
                    pool.submit(verrouille(List.of(second, premier), chacunSonPremierVerrou));

            boolean a = montant.get(60, TimeUnit.SECONDS);
            boolean b = descendant.get(60, TimeUnit.SECONDS);

            // Une seule des deux arrive au bout : l'autre a ete sacrifiee.
            assertThat(a ^ b).isTrue();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("Verrouiller dans le meme ordre : les deux passent, l'une apres l'autre")
    void l_ordre_d_acquisition_evite_l_interblocage() throws Exception {
        Long premier = unExemplaireDisponible("EX-0050");
        Long second = unExemplaireDisponible("EX-0051");

        // Le seul changement : les deux threads trient leurs identifiants.
        List<Long> memeOrdre = List.of(premier, second);

        // Pas de barriere ici, et c'est le point du test : en ordre identique, le second
        // thread ne peut PAS detenir son premier verrou tant que l'autre ne l'a pas libere.
        // Attendre que les deux l'aient obtenu serait attendre l'impossible. L'interblocage
        // n'est donc plus seulement evite : il est devenu structurellement impossible.
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> un = pool.submit(verrouille(memeOrdre, null));
            Future<Boolean> deux = pool.submit(verrouille(memeOrdre, null));

            assertThat(un.get(60, TimeUnit.SECONDS)).isTrue();
            assertThat(deux.get(60, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Verrouille les exemplaires donnes, dans l'ordre donne.
     *
     * @param barriere synchronisation apres le premier verrou, ou {@code null} pour laisser
     *     les threads s'enchainer naturellement
     * @return {@code true} si la transaction a pu etre validee
     */
    private Callable<Boolean> verrouille(List<Long> ids, CyclicBarrier barriere) {
        return () -> {
            try (Session session = sessionFactory().openSession()) {
                session.beginTransaction();

                session.find(Copy.class, ids.get(0), LockModeType.PESSIMISTIC_WRITE);
                if (barriere != null) {
                    barriere.await(60, TimeUnit.SECONDS); // chacun detient son premier verrou
                }
                session.find(Copy.class, ids.get(1), LockModeType.PESSIMISTIC_WRITE);

                session.getTransaction().commit();
                return true;
            } catch (RuntimeException sacrifiee) {
                return false; // PostgreSQL a detecte l'interblocage et a tue cette transaction
            }
        };
    }
}
