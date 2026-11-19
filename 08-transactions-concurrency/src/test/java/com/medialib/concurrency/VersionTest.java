package com.medialib.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Le verrouillage optimiste : aucun verrou pose, mais tout conflit detecte. */
class VersionTest extends ConcurrenceTestBase {

    @Test
    @DisplayName("La colonne version est incrementee, et contrainte dans le WHERE de l'UPDATE")
    void la_colonne_version_est_incrementee_a_chaque_update() {
        Long id = unExemplaireDisponible("EX-0001");

        inTransaction(session -> {
            Copy copy = session.find(Copy.class, id);
            assertThat(copy.getVersion()).isZero();
            sql().reset();

            copy.setBarcode("EX-0001-bis");
            session.flush();

            assertThat(sql().updateCount()).isEqualTo(1);
            // La version est a la fois mise a jour et verifiee : c'est tout le mecanisme.
            assertThat(sql().lastQuery()).contains("set", "version=", "where", "version=");
            assertThat(copy.getVersion()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("Deux sessions concurrentes : la seconde a ecrire leve OptimisticLockException")
    void deux_sessions_concurrentes_levent_optimisticlockexception() {
        Long id = unExemplaireDisponible("EX-0002");

        try (Session premiere = sessionFactory().openSession();
                Session seconde = sessionFactory().openSession()) {

            premiere.beginTransaction();
            seconde.beginTransaction();

            // Les deux lisent la meme version.
            Copy vuePremiere = premiere.find(Copy.class, id);
            Copy vueSeconde = seconde.find(Copy.class, id);
            assertThat(vuePremiere.getVersion()).isEqualTo(vueSeconde.getVersion());

            // La premiere ecrit et valide : la version passe a 1.
            vuePremiere.setBarcode("EX-0002-par-la-premiere");
            premiere.getTransaction().commit();

            // La seconde ecrit sur la base de la version 0, qui n'existe plus.
            vueSeconde.setBarcode("EX-0002-par-la-seconde");
            assertThatThrownBy(() -> seconde.getTransaction().commit())
                    .isInstanceOf(OptimisticLockException.class);
        }

        String gagnant = inTransactionReturning(session ->
                session.find(Copy.class, id).getBarcode());
        assertThat(gagnant).isEqualTo("EX-0002-par-la-premiere");
    }

    @Test
    @DisplayName("OPTIMISTIC_FORCE_INCREMENT : incrementer la version sans rien modifier")
    void optimistic_force_increment_incremente_sans_modification() {
        Long id = unExemplaireDisponible("EX-0003");

        inTransaction(session -> {
            sql().reset();

            // Cas d'usage reel : on modifie un emprunt, et on veut que toute modification
            // concurrente de l'exemplaire soit detectee, bien qu'on ne le modifie pas.
            Copy copy = session.find(Copy.class, id, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            assertThat(copy.isAvailable()).isTrue();
        });

        int version = inTransactionReturning(session -> session.find(Copy.class, id).getVersion());
        assertThat(version).isEqualTo(1); // incrementee, sans qu'aucun champ ait change
    }

    @Test
    @DisplayName("PESSIMISTIC_FORCE_INCREMENT : il verrouille la ligne ET incremente tout de suite")
    void pessimistic_force_increment_verrouille_et_incremente() {
        Long id = unExemplaireDisponible("EX-0005");

        inTransaction(session -> {
            sql().reset();

            session.find(Copy.class, id, LockModeType.PESSIMISTIC_FORCE_INCREMENT);

            // Les deux moities du nom, dans le SQL. D'abord un verrou de ligne en base, comme
            // PESSIMISTIC_WRITE : toute transaction concurrente qui voudrait cette ligne
            // attend. Ensuite un UPDATE de la version, emis immediatement et non au commit.
            assertThat(sql().allNormalized()).anyMatch(s -> s.contains("for no key update"));
            assertThat(sql().updateCount()).isEqualTo(1);
            assertThat(sql().allNormalized())
                    .anyMatch(s -> s.startsWith("update copy") && s.contains("version"));
        });

        int version = inTransactionReturning(session -> session.find(Copy.class, id).getVersion());
        assertThat(version).isEqualTo(1);

        // La difference avec OPTIMISTIC_FORCE_INCREMENT, qui est juste au-dessus : celui-la
        // n'emet aucun verrou et ne decale la detection qu'au commit, ou elle peut echouer.
        // Celui-ci paie un verrou tout de suite pour ne pas avoir a echouer plus tard. On le
        // choisit quand le conflit est probable et qu'un echec en fin de transaction couterait
        // plus cher que l'attente.
    }

    @Test
    @DisplayName("Sans conflit, rien ne se passe : le verrouillage optimiste est gratuit")
    void sans_conflit_le_verrouillage_optimiste_ne_coute_rien() {
        Long id = unExemplaireDisponible("EX-0004");

        inTransaction(session -> {
            sql().reset();
            Copy copy = session.find(Copy.class, id);
            copy.setBarcode("EX-0004-bis");
            session.flush();

            // Aucun "for update", aucun verrou en base : juste un UPDATE contraint.
            assertThat(sql().allNormalized()).noneMatch(s -> s.contains("for update"));
        });
    }
}
