package com.medialib.caching;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le cache de premier niveau, c'est-a-dire le contexte de persistance.
 *
 * <p>Ce n'est pas une optimisation qu'on active : c'est le mecanisme meme de la session. Il
 * garantit qu'une entite donnee n'a qu'une seule instance dans une session, ce dont depend
 * tout le reste — le dirty checking, les associations, l'identite des objets.
 */
class CacheNiveau1Test extends CacheTestBase {

    @Test
    @DisplayName("Deux find dans la meme session rendent la MEME instance, pas une copie")
    void deux_find_dans_la_meme_session_rendent_la_meme_instance() {
        Long id = inTransactionReturning(session -> {
            Loan loan = new Loan(java.time.LocalDate.of(2026, 3, 1));
            session.persist(loan);
            return loan.getId();
        });

        inTransaction(session -> {
            sql().reset();

            Loan premier = session.find(Loan.class, id);
            Loan second = session.find(Loan.class, id);

            assertThat(sql().selectCount()).isEqualTo(1); // une seule requete pour deux find
            assertThat(second).isSameAs(premier);         // identite, pas seulement egalite
        });
    }

    @Test
    @DisplayName("Loan n'est pas cache en L2, et pourtant le L1 fonctionne : il n'est pas optionnel")
    void le_cache_l1_n_est_pas_desactivable() {
        Long id = inTransactionReturning(session -> {
            Loan loan = new Loan(java.time.LocalDate.of(2026, 3, 1));
            session.persist(loan);
            return loan.getId();
        });

        // Loan ne porte aucune annotation de cache : rien en L2.
        inTransaction(session -> {
            sql().reset();
            session.find(Loan.class, id);
            assertThat(sql().selectCount()).isEqualTo(1); // il faut aller en base
            session.find(Loan.class, id);
            assertThat(sql().selectCount()).isEqualTo(1); // mais le L1, lui, est la
        });

        // Nouvelle session : on repart en base, faute de cache L2.
        inTransaction(session -> {
            sql().reset();
            session.find(Loan.class, id);
            assertThat(sql().selectCount()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("clear() vide le contexte : la session oublie ce qu'elle savait")
    void clear_vide_le_cache_de_premier_niveau() {
        Long id = inTransactionReturning(session -> {
            Loan loan = new Loan(java.time.LocalDate.of(2026, 3, 1));
            session.persist(loan);
            return loan.getId();
        });

        inTransaction(session -> {
            Loan premier = session.find(Loan.class, id);
            sql().reset();

            session.clear();

            Loan second = session.find(Loan.class, id);

            assertThat(sql().selectCount()).isEqualTo(1); // il a fallu recharger
            assertThat(second).isNotSameAs(premier);      // et c'est une autre instance
        });
    }
}
