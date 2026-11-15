package com.medialib.associations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le piege le plus silencieux du mapping : {@code equals} et {@code hashCode} fondes sur un
 * identifiant que la base attribue.
 *
 * <p>Tant que l'entite est <em>transient</em>, son id vaut {@code null}. Des qu'Hibernate
 * l'attribue, le {@code hashCode} change. Un {@code HashSet} range ses elements par
 * {@code hashCode} au moment de l'insertion : l'objet reste dans le seau ou il a ete place,
 * mais on le cherche desormais ailleurs. Il devient introuvable dans la collection qui le
 * contient pourtant.
 */
class EqualsHashCodeTest extends AssociationsTestBase {

    @Test
    @DisplayName("Un Set perd l'entite quand hashCode depend de l'id genere")
    void un_set_perd_l_entite_si_hashcode_depend_de_l_id_genere() {
        inTransaction(session -> {
            Author author = new Author("Mariama Ba");
            Set<Author> collection = new HashSet<>();
            collection.add(author);

            assertThat(collection).contains(author); // avant persistance : tout va bien

            session.persist(author);
            session.flush();

            assertThat(author.getId()).isNotNull();
            // Meme instance, meme collection — et pourtant :
            assertThat(collection.contains(author)).isFalse();
        });
    }

    @Test
    @DisplayName("Une cle metier stable resout le probleme")
    void une_cle_metier_stable_survit_a_la_persistance() {
        inTransaction(session -> {
            Member member = new Member("ADH-100", "Fatou Sow");
            Set<Member> collection = new HashSet<>();
            collection.add(member);

            session.persist(member);
            session.flush();

            assertThat(member.getId()).isNotNull();
            assertThat(collection).contains(member); // le numero d'adherent n'a pas bouge
        });
    }

    @Test
    @DisplayName("Deux instances distinctes du meme adherent sont egales par leur cle metier")
    void deux_instances_de_meme_cle_metier_sont_egales() {
        Member premier = new Member("ADH-101", "Awa Diop");
        Member second = new Member("ADH-101", "Awa Diop (saisie corrigee)");

        assertThat(premier).isEqualTo(second);
        assertThat(premier).hasSameHashCodeAs(second);
    }
}
