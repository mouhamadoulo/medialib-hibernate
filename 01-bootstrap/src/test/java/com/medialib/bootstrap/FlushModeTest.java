package com.medialib.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.medialib.support.HibernateTestBase;
import org.hibernate.FlushMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Quand Hibernate ecrit-il reellement ?
 *
 * <p>En {@code AUTO}, une requete qui porte sur une entite en attente declenche un
 * <em>auto-flush</em> : sinon la requete lirait une base qui ignore ce que la session sait
 * deja. En {@code COMMIT}, rien n'est ecrit avant le commit — ce qui peut faire lire des
 * donnees perimees par ses propres soins.
 */
class FlushModeTest extends HibernateTestBase {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {Member.class, Branch.class};
    }

    @Test
    @DisplayName("FlushMode.AUTO : une requete force l'ecriture des entites en attente")
    void flush_mode_auto_declenche_un_auto_flush_avant_la_requete() {
        inTransaction(session -> {
            session.setHibernateFlushMode(FlushMode.AUTO);
            session.persist(new Member("Awa", "Diop", "awa.diop@medialib.sn"));
            sql().reset();

            Long nombre = session.createQuery("select count(m) from Member m", Long.class)
                    .getSingleResult();

            assertThat(sql().insertCount()).isEqualTo(1); // l'auto-flush a eu lieu
            assertThat(nombre).isEqualTo(1L);             // la requete voit le nouveau membre
        });
    }

    @Test
    @DisplayName("FlushMode.COMMIT : la requete ne voit pas ce que la session sait deja")
    void flush_mode_commit_differe_les_ecritures() {
        inTransaction(session -> {
            session.setHibernateFlushMode(FlushMode.COMMIT);
            session.persist(new Member("Awa", "Diop", "awa.diop@medialib.sn"));
            sql().reset();

            Long nombre = session.createQuery("select count(m) from Member m", Long.class)
                    .getSingleResult();

            assertThat(sql().insertCount()).isZero(); // aucune ecriture
            assertThat(nombre).isZero();              // le piege : la requete ne le voit pas
        });

        // L'insertion a bien eu lieu, au commit.
        Long total = inTransactionReturning(session ->
                session.createQuery("select count(m) from Member m", Long.class).getSingleResult());
        assertThat(total).isEqualTo(1L);
    }
}
