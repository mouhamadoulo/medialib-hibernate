package com.medialib.collections;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pourquoi la meme {@code List<String>} coute cher ou pas, selon une seule annotation.
 *
 * <p>Un <em>bag</em> est une liste sans index : la table de collection n'a que la cle du parent
 * et la valeur. Hibernate ne peut donc pas designer une ligne precise pour la modifier — il
 * efface tout et reinsere. Avec {@code @OrderColumn}, chaque ligne a une position : la
 * modification devient chirurgicale.
 */
class BagVsListTest extends CollectionsTestBase {

    private Long unMediaAvecTroisNotesEtTroisEntrees() {
        return inTransactionReturning(session -> {
            Media media = new Media("Une si longue lettre");
            media.getNotes().add("exemplaire abime");
            media.getNotes().add("couverture refaite");
            media.getNotes().add("don de la famille Ba");
            media.getAgenda().add("inventaire janvier");
            media.getAgenda().add("inventaire avril");
            media.getAgenda().add("inventaire juillet");
            session.persist(media);
            return media.getId();
        });
    }

    @Test
    @DisplayName("Modifier un bag : Hibernate efface toute la collection et la reinsere")
    void modifier_un_bag_supprime_tout_et_reinsere() {
        Long id = unMediaAvecTroisNotesEtTroisEntrees();

        inTransaction(session -> {
            Media media = session.find(Media.class, id);
            media.getNotes().size(); // chargement de la collection
            sql().reset();

            media.getNotes().add("relie en 2026"); // UN ajout

            session.flush();

            // Un DELETE de toute la table de collection, puis QUATRE INSERT.
            assertThat(sql().deleteCount()).isEqualTo(1);
            assertThat(sql().allNormalized()).anyMatch(s -> s.startsWith("delete from media_note"));
            assertThat(sql().insertCount()).isEqualTo(4);
        });
    }

    @Test
    @DisplayName("Modifier une liste indexee : un seul INSERT, rien n'est efface")
    void une_list_indexee_par_ordercolumn_met_a_jour_ciblement() {
        Long id = unMediaAvecTroisNotesEtTroisEntrees();

        inTransaction(session -> {
            Media media = session.find(Media.class, id);
            media.getAgenda().size();
            sql().reset();

            media.getAgenda().add("inventaire octobre"); // UN ajout, en fin de liste

            session.flush();

            assertThat(sql().deleteCount()).isZero();
            assertThat(sql().insertCount()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("La liste indexee restitue l'ordre d'insertion, le bag ne le garantit pas")
    void la_colonne_de_position_preserve_l_ordre() {
        Long id = unMediaAvecTroisNotesEtTroisEntrees();

        inTransaction(session -> {
            Media media = session.find(Media.class, id);

            assertThat(media.getAgenda())
                    .containsExactly("inventaire janvier", "inventaire avril", "inventaire juillet");
        });
    }
}
