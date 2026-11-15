package com.medialib.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.medialib.support.HibernateTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le <em>dirty checking</em> : Hibernate compare l'etat d'une entite managee a l'instantane
 * pris au chargement, et emet l'UPDATE tout seul. C'est ce qui surprend le plus quand on
 * arrive du JDBC — il n'y a pas de methode « save » a appeler.
 */
class DirtyCheckingTest extends HibernateTestBase {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {Member.class, Branch.class};
    }

    private Long unMembreEnBase() {
        return inTransactionReturning(session -> {
            Member member = new Member("Awa", "Diop", "awa.diop@medialib.sn");
            session.persist(member);
            return member.getId();
        });
    }

    @Test
    @DisplayName("Aucun appel a save : modifier une entite managee suffit")
    void modifier_une_entite_managee_suffit_a_la_mettre_a_jour() {
        Long id = unMembreEnBase();

        inTransaction(session -> {
            Member member = session.find(Member.class, id);
            sql().reset();

            member.setEmail("awa.diop@ville.sn"); // et c'est tout

            session.flush();
            assertThat(sql().updateCount()).isEqualTo(1);
        });

        String email = inTransactionReturning(session -> session.find(Member.class, id).getEmail());
        assertThat(email).isEqualTo("awa.diop@ville.sn");
    }

    @Test
    @DisplayName("Une entite detachee n'est plus suivie : la modification est perdue")
    void une_entite_detachee_n_est_plus_suivie() {
        Long id = unMembreEnBase();

        inTransaction(session -> {
            Member member = session.find(Member.class, id);
            session.detach(member);
            sql().reset();

            member.setEmail("perdu@nulle-part.sn");

            session.flush();
            assertThat(sql().updateCount()).isZero();
        });

        String email = inTransactionReturning(session -> session.find(Member.class, id).getEmail());
        assertThat(email).isEqualTo("awa.diop@medialib.sn"); // inchange
    }

    @Test
    @DisplayName("merge rattache une copie detachee et reporte ses modifications")
    void merge_reattache_et_met_a_jour() {
        Long id = unMembreEnBase();

        Member detache = inTransactionReturning(session -> {
            Member member = session.find(Member.class, id);
            session.detach(member);
            return member;
        });
        detache.setEmail("awa.diop@reseau.sn");

        inTransaction(session -> {
            sql().reset();

            Member rattache = session.merge(detache);

            // merge recharge l'etat en base avant de reporter les modifications
            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(rattache).isNotSameAs(detache); // merge rend une AUTRE instance

            session.flush();
            assertThat(sql().updateCount()).isEqualTo(1);
        });

        String email = inTransactionReturning(session -> session.find(Member.class, id).getEmail());
        assertThat(email).isEqualTo("awa.diop@reseau.sn");
    }
}
