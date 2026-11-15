package com.medialib.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le choix du generateur d'identifiant n'est pas un detail : il decide du moment ou Hibernate
 * doit ecrire, donc de sa capacite a differer et regrouper les insertions.
 */
class IdentifiantsTest extends MappingTestBase {

    @Test
    @DisplayName("IDENTITY force l'INSERT des le persist : plus rien n'est differable")
    void identity_insere_immediatement_et_interdit_tout_regroupement() {
        inTransaction(session -> {
            sql().reset();

            session.persist(new Publisher("Presence Africaine"));
            session.persist(new Publisher("Gallimard"));
            session.persist(new Publisher("Nouvelles Editions Africaines"));

            // Trois INSERT deja partis, avant meme le flush : la base seule connait la cle.
            assertThat(sql().insertCount()).isEqualTo(3);
        });
    }

    @Test
    @DisplayName("SEQUENCE connait la cle d'avance : les INSERT attendent le flush")
    void sequence_differe_les_insertions_jusqu_au_flush() {
        inTransaction(session -> {
            sql().reset();

            session.persist(new Media("Une si longue lettre", new Isbn("978-2-7236-0430-9"),
                    MediaFormat.BOOK, 165));
            session.persist(new Media("L'Aventure ambigue", new Isbn("978-2-264-03061-9"),
                    MediaFormat.BOOK, 191));
            session.persist(new Media("Les Bouts de bois de Dieu", new Isbn("978-2-266-11478-1"),
                    MediaFormat.BOOK, 379));

            // Les sequences ont ete interrogees, mais rien n'est encore ecrit.
            assertThat(sql().insertCount()).isZero();
            assertThat(sql().selectCount()).isEqualTo(3); // nextval, une fois par entite

            session.flush();

            assertThat(sql().insertCount()).isEqualTo(3);
        });
    }

    @Test
    @DisplayName("UUID : l'identifiant existe avant tout dialogue avec la base")
    void uuid_genere_un_identifiant_cote_java() {
        inTransaction(session -> {
            Fine fine = new Fine(Money.ofEuros("2.50"), "retard de 5 jours");
            sql().reset();

            session.persist(fine);

            assertThat(fine.getId()).isNotNull();
            assertThat(sql().count()).isZero(); // aucun aller-retour : ni sequence, ni insert
        });
    }
}
