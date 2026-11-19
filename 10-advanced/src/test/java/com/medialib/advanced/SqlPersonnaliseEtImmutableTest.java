package com.medialib.advanced;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Reprendre la main sur le SQL d'ecriture, et y renoncer tout a fait.
 *
 * <p>Trois annotations qui agissent toutes sur l'ordre d'ecriture, chacune a sa facon :
 * {@code @SQLInsert} / {@code @SQLUpdate} remplacent l'enonce par le notre,
 * {@code @DynamicInsert} ne change que la liste des colonnes, et {@code @Immutable} supprime
 * l'ordre d'ecriture purement et simplement.
 */
class SqlPersonnaliseEtImmutableTest extends AvanceTestBase {

    @Test
    @DisplayName("@SQLInsert : c'est notre enonce qui part, fonction SQL comprise")
    void sql_insert_remplace_l_enonce_genere() {
        inTransaction(session -> {
            sql().reset();
            session.persist(new Fine(1L, new BigDecimal("2.50"), "retard de 5 jours"));
            session.flush();

            // Hibernate n'a rien genere : il a emis notre chaine telle quelle, avec son
            // upper() que son SQL genere n'aurait jamais produit.
            assertThat(sql().insertCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("upper(?)");
        });

        String motif = inTransactionReturning(session -> session.createNativeQuery(
                "select reason from fine where id = 1", String.class).getSingleResult());

        // La preuve que la base a bien execute notre fonction.
        assertThat(motif).isEqualTo("RETARD DE 5 JOURS");
    }

    @Test
    @DisplayName("@SQLUpdate : meme reprise en main sur la modification")
    void sql_update_remplace_l_enonce_genere() {
        inTransaction(session -> session.persist(
                new Fine(2L, new BigDecimal("2.50"), "retard")));

        inTransaction(session -> {
            Fine fine = session.find(Fine.class, 2L);
            sql().reset();
            fine.setReason("perte du document");
            session.flush();

            assertThat(sql().updateCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("upper(?)");
        });

        String motif = inTransactionReturning(session -> session.createNativeQuery(
                "select reason from fine where id = 2", String.class).getSingleResult());
        assertThat(motif).isEqualTo("PERTE DU DOCUMENT");

        // Le prix de ces deux annotations : l'ordre des parametres est celui qu'Hibernate
        // attend, pas celui qu'on croit. Ajouter une colonne a l'entite sans retoucher la
        // chaine decale silencieusement les liaisons — aucune erreur de compilation, et une
        // base qui se remplit de travers.
    }

    @Test
    @DisplayName("@Immutable : la modification est ignoree, sans erreur et sans UPDATE")
    void immutable_fait_disparaitre_l_ordre_d_ecriture() {
        inTransaction(session -> session.persist(new ArchivedLoan(1L, "Oeuvre archivee")));

        inTransaction(session -> {
            ArchivedLoan archive = session.find(ArchivedLoan.class, 1L);
            sql().reset();

            archive.setMediaTitle("Titre corrige");
            session.flush();

            // Aucun UPDATE. Hibernate ne refuse pas la modification, il ne la voit pas : une
            // entite @Immutable est exclue du dirty checking.
            assertThat(sql().updateCount()).isZero();
            assertThat(sql().count()).isZero();
        });

        String titre = inTransactionReturning(session -> session.createNativeQuery(
                "select media_title from archived_loan where id = 1", String.class)
                .getSingleResult());

        // L'objet en memoire portait « Titre corrige », la base porte toujours l'ancien. Le
        // silence est le danger : aucune exception ne previent que l'ecriture a ete perdue.
        // On reserve donc cette annotation a ce qui est vraiment en lecture seule — une table
        // d'archives, une entite de @Subselect (voir le chapitre 06).
        assertThat(titre).isEqualTo("Oeuvre archivee");
    }

    @Test
    @DisplayName("@DynamicInsert : les colonnes nulles quittent l'INSERT, et le defaut s'applique enfin")
    void dynamic_insert_laisse_la_base_appliquer_ses_defauts() {
        inTransaction(session -> {
            sql().reset();
            session.persist(new MemberProfile(1L, "Awa Diop", null));
            session.flush();

            // La colonne laissee a null est absente de l'enonce : Hibernate ne liste que les
            // colonnes reellement renseignees.
            assertThat(sql().lastQuery())
                    .contains("insert into member_profile")
                    .doesNotContain("language");
        });

        String langue = inTransactionReturning(session -> session.createNativeQuery(
                "select language from member_profile where id = 1", String.class)
                .getSingleResult());

        // Et c'est ce qui rend enfin un @ColumnDefault utile. Au chapitre 02, la meme
        // declaration sans @DynamicInsert laissait partir un NULL explicite qui ecrasait le
        // defaut : la colonne finissait a null. Ici elle vaut 'fr'.
        assertThat(langue).isEqualTo("fr");

        // Le cout est reel : un enonce different par combinaison de champs renseignes, donc
        // un cache de requetes preparees qui se fragmente. A reserver aux entites larges et
        // majoritairement creuses.
    }
}
