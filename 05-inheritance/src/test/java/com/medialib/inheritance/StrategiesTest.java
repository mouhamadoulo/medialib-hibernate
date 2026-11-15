package com.medialib.inheritance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.medialib.inheritance.joined.JoinedBook;
import com.medialib.inheritance.single.SingleBook;
import com.medialib.inheritance.tpc.TpcBook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Ce que chaque strategie ecrit, et ce qu'elle lit. */
class StrategiesTest extends HeritageTestBase {

    @Test
    @DisplayName("SINGLE_TABLE : une insertion, une table, un discriminant")
    void une_seule_table_avec_colonne_discriminante() {
        inTransaction(session -> {
            sql().reset();

            session.persist(new SingleBook("Une si longue lettre", "9782723604309", 165));
            session.flush();

            assertThat(sql().insertCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("insert into single_media", "media_type");
        });
    }

    @Test
    @DisplayName("SINGLE_TABLE : charger un livre ne joint rien")
    void single_table_charge_sans_jointure() {
        Long id = inTransactionReturning(session -> {
            SingleBook book = new SingleBook("L'Aventure ambigue", "9782264030619", 191);
            session.persist(book);
            return book.getId();
        });

        inTransaction(session -> {
            sql().reset();
            session.find(SingleBook.class, id);

            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).doesNotContain("join");
        });
    }

    @Test
    @DisplayName("SINGLE_TABLE : la contrainte NOT NULL sur une colonne de sous-classe est impossible")
    void les_colonnes_des_sous_classes_sont_forcement_nullables() {
        String nullable = inTransactionReturning(session -> session.createNativeQuery(
                "select is_nullable from information_schema.columns "
                        + "where table_name = 'single_media' and column_name = 'page_count'",
                String.class).getSingleResult());

        // Hibernate n'a pas eu le choix : un DVD occupe la meme table et n'a pas de pages.
        assertThat(nullable).isEqualTo("YES");

        // La meme colonne, en JOINED, est NOT NULL.
        String enJoined = inTransactionReturning(session -> session.createNativeQuery(
                "select is_nullable from information_schema.columns "
                        + "where table_name = 'joined_book' and column_name = 'page_count'",
                String.class).getSingleResult());
        assertThat(enJoined).isEqualTo("NO");
    }

    @Test
    @DisplayName("JOINED : une insertion touche deux tables")
    void une_insertion_touche_deux_tables() {
        inTransaction(session -> {
            sql().reset();

            session.persist(new JoinedBook("Une si longue lettre", "9782723604309", 165));
            session.flush();

            assertThat(sql().insertCount()).isEqualTo(2);
            assertThat(sql().allNormalized())
                    .anyMatch(s -> s.startsWith("insert into joined_media"))
                    .anyMatch(s -> s.startsWith("insert into joined_book"));
        });
    }

    @Test
    @DisplayName("JOINED : charger un livre exige une jointure")
    void charger_un_book_fait_une_jointure() {
        Long id = inTransactionReturning(session -> {
            JoinedBook book = new JoinedBook("L'Aventure ambigue", "9782264030619", 191);
            session.persist(book);
            return book.getId();
        });

        inTransaction(session -> {
            sql().reset();
            JoinedBook relu = session.find(JoinedBook.class, id);

            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("join");
            assertThat(relu.getPageCount()).isEqualTo(191);
        });
    }

    @Test
    @DisplayName("JOINED : la contrainte NOT NULL fait son travail")
    void joined_laisse_la_base_garantir_l_integrite() {
        assertThatThrownBy(() -> inTransaction(session -> {
            session.persist(new JoinedBook("Sans pagination", "9780000000000", null));
            session.flush();
        })).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("TABLE_PER_CLASS : une seule table touchee, qui repete les colonnes du parent")
    void table_per_class_insere_dans_une_table_autonome() {
        inTransaction(session -> {
            sql().reset();

            session.persist(new TpcBook("Une si longue lettre", "9782723604309", 165));
            session.flush();

            assertThat(sql().insertCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("insert into tpc_book", "title");
        });
    }
}
