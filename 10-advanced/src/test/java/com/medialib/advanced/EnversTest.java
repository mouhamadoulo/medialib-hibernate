package com.medialib.advanced;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Envers : l'historique complet, pour le prix d'une annotation.
 *
 * <p>A chaque modification d'une entite {@code @Audited}, Envers ecrit une ligne dans une table
 * {@code _aud} et la relie a une revision. On peut ensuite relire l'etat de n'importe quelle
 * entite a n'importe quel moment de son passe.
 */
class EnversTest extends AvanceTestBase {

    @AfterEach
    void nettoieLUtilisateur() {
        UtilisateurCourant.effacer();
    }

    @Test
    @DisplayName("Chaque modification cree une revision")
    void une_modification_cree_une_revision() {
        Long id = inTransactionReturning(session -> {
            Loan loan = new Loan("Awa Diop", LocalDate.of(2026, 1, 10));
            session.persist(loan);
            return loan.getId();
        });

        inTransaction(session ->
                session.find(Loan.class, id).setReturnedOn(LocalDate.of(2026, 1, 24)));

        inTransaction(session -> {
            AuditReader lecteur = AuditReaderFactory.get(session);
            List<Number> revisions = lecteur.getRevisions(Loan.class, id);

            assertThat(revisions).hasSize(2); // la creation, puis le retour
        });
    }

    @Test
    @DisplayName("On relit l'etat de l'entite a une revision donnee")
    void on_relit_l_etat_a_une_revision_donnee() {
        Long id = inTransactionReturning(session -> {
            Loan loan = new Loan("Awa Diop", LocalDate.of(2026, 1, 10));
            session.persist(loan);
            return loan.getId();
        });

        inTransaction(session ->
                session.find(Loan.class, id).setReturnedOn(LocalDate.of(2026, 1, 24)));

        inTransaction(session -> {
            AuditReader lecteur = AuditReaderFactory.get(session);
            List<Number> revisions = lecteur.getRevisions(Loan.class, id);

            Loan avant = lecteur.find(Loan.class, id, revisions.get(0));
            Loan apres = lecteur.find(Loan.class, id, revisions.get(1));

            assertThat(avant.getReturnedOn()).isNull();            // pas encore rendu
            assertThat(apres.getReturnedOn()).isEqualTo(LocalDate.of(2026, 1, 24));
        });
    }

    @Test
    @DisplayName("La revision porte l'auteur, via une RevisionEntity personnalisee")
    void la_revision_porte_l_auteur() {
        UtilisateurCourant.definir("agent.dakar");

        Long id = inTransactionReturning(session -> {
            Loan loan = new Loan("Ibrahima Fall", LocalDate.of(2026, 2, 1));
            session.persist(loan);
            return loan.getId();
        });

        inTransaction(session -> {
            AuditReader lecteur = AuditReaderFactory.get(session);
            Number revision = lecteur.getRevisions(Loan.class, id).get(0);

            RevisionAvecAuteur info = lecteur.findRevision(RevisionAvecAuteur.class, revision);

            assertThat(info.getAuteur()).isEqualTo("agent.dakar");
            assertThat(info.getRevisionDate()).isNotNull();
        });
    }

    @Test
    @DisplayName("L'historique est interrogeable comme n'importe quelle donnee")
    void l_historique_est_interrogeable() {
        inTransaction(session -> {
            session.persist(new Loan("Awa Diop", LocalDate.of(2026, 1, 10)));
            session.persist(new Loan("Ibrahima Fall", LocalDate.of(2026, 1, 11)));
        });

        inTransaction(session -> {
            AuditReader lecteur = AuditReaderFactory.get(session);

            List<?> empruntsDAwa = lecteur.createQuery()
                    .forRevisionsOfEntity(Loan.class, true, true)
                    .add(AuditEntity.property("memberName").eq("Awa Diop"))
                    .getResultList();

            assertThat(empruntsDAwa).hasSize(1);
        });
    }

    @Test
    @DisplayName("La table d'audit existe, et l'entite non auditee n'en a pas")
    void seules_les_entites_auditees_ont_une_table_d_audit() {
        Long auditees = inTransactionReturning(session -> session.createNativeQuery(
                "select count(*) from information_schema.tables "
                        + "where table_schema = current_schema() and table_name in ('loan_aud', 'member_aud')",
                Long.class).getSingleResult());
        assertThat(auditees).isEqualTo(2L);

        // Media et Copy ne portent pas @Audited : aucune table d'audit pour elles.
        Long nonAuditees = inTransactionReturning(session -> session.createNativeQuery(
                "select count(*) from information_schema.tables "
                        + "where table_schema = current_schema() and table_name in ('media_aud', 'copy_aud')",
                Long.class).getSingleResult());
        assertThat(nonAuditees).isZero();
    }
}
