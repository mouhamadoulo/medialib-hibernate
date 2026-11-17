package com.medialib.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code @IdClass} et {@code @EmbeddedId} : meme cle composite, meme schema, deux facons
 * d'ecrire — et une difference qui ne se voit qu'en HQL.
 *
 * <p>Le chapitre montre {@code @EmbeddedId} sur {@code Loan} (voir {@link LoanKey}). Ici la
 * meme idee par l'autre syntaxe : les colonnes de la cle sont des champs <em>de l'entite</em>,
 * et une classe separee ne sert qu'a les transporter.
 */
class CleCompositeIdClassTest extends MappingTestBase {

    private static final LocalDate DEBUT = LocalDate.of(2026, 1, 1);

    @Test
    @DisplayName("@IdClass produit le meme schema que @EmbeddedId : une cle primaire a deux colonnes")
    void id_class_produit_une_cle_primaire_composite() {
        assertThat(colonnesDeLaClePrimaire("subscription"))
                .containsExactlyInAnyOrder("member_id", "started_on");

        // Celle de Loan, par @EmbeddedId, a trois colonnes. Les deux syntaxes donnent bien une
        // cle primaire composite — le choix est une affaire de code, pas de schema.
        assertThat(colonnesDeLaClePrimaire("loan"))
                .containsExactlyInAnyOrder("member_id", "copy_id", "loaned_on");
    }

    @Test
    @DisplayName("La difference est en HQL : les champs de la cle s'adressent directement")
    void id_class_expose_ses_champs_directement_en_hql() {
        inTransaction(session -> session.persist(new Subscription(1L, DEBUT, "annuel")));

        inTransaction(session -> {
            sql().reset();

            // Avec @IdClass, memberId est un champ de Subscription : on l'ecrit tel quel.
            Subscription trouvee = session.createQuery(
                    "from Subscription s where s.memberId = :m and s.startedOn = :d",
                    Subscription.class)
                    .setParameter("m", 1L)
                    .setParameter("d", DEBUT)
                    .getSingleResult();

            assertThat(trouvee.getPlan()).isEqualTo("annuel");
            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("where", "member_id=?", "started_on=?");
        });
    }

    @Test
    @DisplayName("Avec @EmbeddedId il faut au contraire traverser la cle")
    void embedded_id_oblige_a_traverser_la_cle_en_hql() {
        inTransaction(session ->
                session.persist(new Loan(new LoanKey(1L, 7L, DEBUT), DEBUT.plusDays(21))));

        inTransaction(session -> {
            sql().reset();

            // `l.memberId` ne compilerait pas ici : le champ n'existe pas sur Loan, il vit
            // dans LoanKey. C'est toute la difference entre les deux syntaxes, et la raison de
            // preferer @IdClass quand les requetes filtrent souvent sur un seul morceau de la
            // cle — le code reste plus court, au prix de champs dupliques entre l'entite et la
            // classe de cle.
            Loan trouve = session.createQuery(
                    "from Loan l where l.id.memberId = :m and l.id.copyId = :c", Loan.class)
                    .setParameter("m", 1L)
                    .setParameter("c", 7L)
                    .getSingleResult();

            assertThat(trouve.getDueOn()).isEqualTo(DEBUT.plusDays(21));

            // Le SQL, lui, est le meme : les deux syntaxes filtrent sur les memes colonnes.
            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("member_id=?", "copy_id=?");
        });
    }

    private java.util.List<String> colonnesDeLaClePrimaire(String table) {
        return inTransactionReturning(session -> session.createNativeQuery(
                "select kcu.column_name from information_schema.table_constraints tc "
                        + "join information_schema.key_column_usage kcu "
                        + "  on kcu.constraint_name = tc.constraint_name "
                        + "where tc.table_schema = current_schema() "
                        + "and tc.table_name = :table and tc.constraint_type = 'PRIMARY KEY'",
                String.class)
                .setParameter("table", table)
                .getResultList());
    }
}
