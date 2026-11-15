package com.medialib.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.medialib.support.HibernateTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** L'etat <em>removed</em> : marque pour suppression, supprime au flush. */
class RemoveTest extends HibernateTestBase {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {Member.class, Branch.class};
    }

    @Test
    @DisplayName("remove marque l'entite ; le DELETE part au flush")
    void remove_supprime_au_flush() {
        Long id = inTransactionReturning(session -> {
            Member member = new Member("Awa", "Diop", "awa.diop@medialib.sn");
            session.persist(member);
            return member.getId();
        });

        inTransaction(session -> {
            Member member = session.find(Member.class, id);
            sql().reset();

            session.remove(member);

            assertThat(sql().deleteCount()).isZero();

            session.flush();

            assertThat(sql().deleteCount()).isEqualTo(1);
        });

        Member disparu = inTransactionReturning(session -> session.find(Member.class, id));
        assertThat(disparu).isNull();
    }
}
