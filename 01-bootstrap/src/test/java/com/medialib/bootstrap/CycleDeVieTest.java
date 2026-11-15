package com.medialib.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.medialib.support.HibernateTestBase;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les quatre etats d'une entite — <em>transient</em>, <em>managed</em>, <em>detached</em>,
 * <em>removed</em> — et ce que chaque transition declenche reellement en base.
 */
class CycleDeVieTest extends HibernateTestBase {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {Member.class, Branch.class};
    }

    @Test
    @DisplayName("Un objet transient est un objet Java ordinaire : Hibernate ne le connait pas")
    void une_entite_transient_ne_declenche_aucun_sql() {
        inTransaction(session -> {
            Member member = new Member("Awa", "Diop", "awa.diop@medialib.sn");

            assertThat(member.getId()).isNull();
            assertThat(sql().count()).isZero();
        });
    }

    @Test
    @DisplayName("persist rend l'entite managee, mais n'ecrit qu'au flush")
    void persist_insere_au_flush_pas_a_l_appel() {
        inTransaction(session -> {
            Member member = new Member("Awa", "Diop", "awa.diop@medialib.sn");

            session.persist(member);

            // L'identifiant est deja connu : le generateur SEQUENCE a interroge la sequence.
            assertThat(member.getId()).isNotNull();
            assertThat(sql().insertCount()).isZero();

            session.flush();

            assertThat(sql().insertCount()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("Le contexte de persistance garantit l'identite : deux find rendent la meme instance")
    void find_sur_entite_deja_chargee_ne_requete_pas() {
        Long id = inTransactionReturning(session -> {
            Member member = new Member("Ibrahima", "Fall", "ibrahima.fall@medialib.sn");
            session.persist(member);
            return member.getId();
        });

        inTransaction(session -> {
            sql().reset();

            Member premier = session.find(Member.class, id);
            assertThat(sql().selectCount()).isEqualTo(1);

            Member second = session.find(Member.class, id);

            assertThat(sql().selectCount()).isEqualTo(1); // aucune seconde requete
            assertThat(second).isSameAs(premier);         // meme instance, pas seulement egale
        });
    }

    @Test
    @DisplayName("getReference rend un proxy : la requete n'est emise qu'au premier acces")
    void getReference_ne_requete_pas_avant_acces() {
        Long id = inTransactionReturning(session -> {
            Member member = new Member("Fatou", "Sow", "fatou.sow@medialib.sn");
            session.persist(member);
            return member.getId();
        });

        inTransaction(session -> {
            sql().reset();

            Member proxy = session.getReference(Member.class, id);

            assertThat(Hibernate.isInitialized(proxy)).isFalse();
            assertThat(sql().selectCount()).isZero();
            assertThat(proxy.getId()).isEqualTo(id); // l'identifiant est deja connu du proxy
            assertThat(sql().selectCount()).isZero();

            assertThat(proxy.getLastName()).isEqualTo("Sow"); // premier acces a un vrai champ

            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(Hibernate.isInitialized(proxy)).isTrue();
        });
    }
}
