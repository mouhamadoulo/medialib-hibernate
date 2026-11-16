package com.medialib.inheritance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.medialib.inheritance.joined.JoinedBook;
import com.medialib.inheritance.single.SingleBook;
import com.medialib.inheritance.tpc.TpcBook;
import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
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
    @DisplayName("SINGLE_TABLE : la colonne de sous-classe doit rester nullable, JOINED non")
    void les_colonnes_des_sous_classes_sont_forcement_nullables() {
        // SingleBook.pageCount ne declare PAS nullable = false, et il ne peut pas : la table
        // est partagee avec les DVD et les vinyles, qui n'ont pas de pages.
        assertThat(nullabiliteDe("single_media", "page_count")).isEqualTo("YES");

        // La meme propriete, en JOINED, vit dans sa propre table : la contrainte y tient.
        assertThat(nullabiliteDe("joined_book", "page_count")).isEqualTo("NO");

        // Ce qui arrive quand on exige quand meme la contrainte en SINGLE_TABLE est demontre
        // a part, sur une hierarchie dediee : voir NotNullEnSingleTableTest. Resume : Hibernate
        // ne l'ignore pas, il la cree — et ce sont les autres sous-classes qui cassent.
    }

    private String nullabiliteDe(String table, String colonne) {
        return inTransactionReturning(session -> session.createNativeQuery(
                "select is_nullable from information_schema.columns "
                        + "where table_schema = current_schema() "
                        + "and table_name = :table and column_name = :colonne",
                String.class)
                .setParameter("table", table)
                .setParameter("colonne", colonne)
                .getSingleResult());
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
    @DisplayName("JOINED : Hibernate refuse l'insertion AVANT que la base ne soit sollicitee")
    void joined_rejette_la_propriete_nulle_des_le_flush() {
        assertThatThrownBy(() -> inTransaction(session -> {
            session.persist(new JoinedBook("Sans pagination", "9780000000000", null));
            session.flush();
        }))
                // Et non une ContraintViolationException de PostgreSQL : avec
                // @Column(nullable = false), Hibernate verifie lui-meme la nullite au flush
                // et n'emet jamais l'INSERT. La contrainte en base n'est donc qu'une seconde
                // ligne de defense — celle qui protege des ecritures faites hors d'Hibernate.
                .isInstanceOf(PropertyValueException.class)
                .hasMessageContaining("not-null property");
    }

    @Test
    @DisplayName("JOINED : et la base aussi refuserait, si on la sollicitait directement")
    void joined_laisse_la_base_garantir_l_integrite() {
        // En SQL natif, Hibernate ne verifie rien : c'est PostgreSQL qui tranche.
        assertThatThrownBy(() -> inTransaction(session -> session.createNativeMutationQuery(
                "insert into joined_book (media_id, isbn, page_count) values (-1, 'x', null)")
                .executeUpdate()))
                .isInstanceOf(ConstraintViolationException.class);
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
