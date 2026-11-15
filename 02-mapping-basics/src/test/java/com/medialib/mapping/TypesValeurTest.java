package com.medialib.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les types valeur : {@code @Embeddable}, {@code AttributeConverter}, {@code UserType} et
 * cle composite. Tous repondent a la meme question — comment persister quelque chose qui n'a
 * pas d'identite propre.
 */
class TypesValeurTest extends MappingTestBase {

    @Test
    @DisplayName("Un @Embeddable est aplati dans la table de l'entite : aucune jointure")
    void une_adresse_est_aplatie_dans_la_table_du_membre() {
        Long id = inTransactionReturning(session -> {
            Member member = new Member("Awa Diop",
                    new Address("12 rue Felix Faure", "Dakar", "11000"));
            session.persist(member);
            return member.getId();
        });

        inTransaction(session -> {
            sql().reset();

            Member member = session.find(Member.class, id);

            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("street", "city", "postal_code");
            assertThat(sql().lastQuery()).doesNotContain("join");
            assertThat(member.getAddress().getCity()).isEqualTo("Dakar");
        });
    }

    @Test
    @DisplayName("Un AttributeConverter persiste le montant en centimes, pas en decimal")
    void money_est_persiste_en_centimes() {
        UUID id = inTransactionReturning(session -> {
            Fine fine = new Fine(Money.ofEuros("2.50"), "retard de 5 jours");
            session.persist(fine);
            return fine.getId();
        });

        Object brut = inTransactionReturning(session ->
                session.createNativeQuery("select amount_cents from fine where id = :id", Object.class)
                        .setParameter("id", id)
                        .getSingleResult());

        assertThat(((Number) brut).longValue()).isEqualTo(250L);

        Money relu = inTransactionReturning(session -> session.find(Fine.class, id).getAmount());
        assertThat(relu).isEqualTo(Money.ofEuros("2.50"));
    }

    @Test
    @DisplayName("Un UserType normalise avant l'ecriture : l'ISBN atteint la base sans tirets")
    void isbn_est_persiste_normalise() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("Une si longue lettre", new Isbn("978-2-7236-0430-9"),
                    MediaFormat.BOOK, 165);
            session.persist(media);
            return media.getId();
        });

        String brut = inTransactionReturning(session ->
                session.createNativeQuery("select isbn from media where id = :id", String.class)
                        .setParameter("id", id)
                        .getSingleResult());

        assertThat(brut).isEqualTo("9782723604309"); // les tirets de la saisie ont disparu

        Isbn relu = inTransactionReturning(session -> session.find(Media.class, id).getIsbn());
        assertThat(relu).isEqualTo(new Isbn("9782723604309"));
        assertThat(relu).isEqualTo(new Isbn("978-2-7236-0430-9")); // meme valeur, saisie differente
    }

    @Test
    @DisplayName("Une cle composite se traduit en WHERE a trois colonnes")
    void une_cle_composite_est_traduite_en_clause_where_a_plusieurs_colonnes() {
        LoanKey cle = new LoanKey(1L, 42L, LocalDate.of(2026, 3, 1));

        inTransaction(session -> session.persist(new Loan(cle, LocalDate.of(2026, 3, 22))));

        inTransaction(session -> {
            sql().reset();

            Loan loan = session.find(Loan.class, cle);

            assertThat(loan).isNotNull();
            assertThat(loan.getDueOn()).isEqualTo(LocalDate.of(2026, 3, 22));
            assertThat(sql().lastQuery()).contains("member_id", "copy_id", "loaned_on");
        });
    }
}
