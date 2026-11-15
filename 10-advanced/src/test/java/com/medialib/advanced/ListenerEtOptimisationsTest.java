package com.medialib.advanced;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Listener d'evenements, {@code @NaturalId} et {@code @DynamicUpdate}. */
class ListenerEtOptimisationsTest extends AvanceTestBase {

    @Test
    @DisplayName("Un listener PreInsert horodate sans qu'aucun appelant ne s'en occupe")
    void un_listener_preinsert_horodate_automatiquement() {
        Long id = inTransactionReturning(session -> {
            // Personne n'appelle setCreatedAt.
            Media media = new Media("9782723604309", "Une si longue lettre");
            session.persist(media);
            return media.getId();
        });

        // En memoire ET en base : le listener a aussi ecrit dans l'etat de l'INSERT.
        inTransaction(session -> {
            Media relu = session.find(Media.class, id);
            assertThat(relu.getCreatedAt()).isNotNull();
        });

        Long renseignes = inTransactionReturning(session -> session.createNativeQuery(
                "select count(*) from media where id = :id and created_at is not null", Long.class)
                .setParameter("id", id).getSingleResult());
        assertThat(renseignes).isEqualTo(1L);
    }

    @Test
    @DisplayName("@NaturalId : une recherche par cle metier, et son propre cache de resolution")
    void une_recherche_par_naturalid_passe_par_son_cache() {
        inTransaction(session ->
                session.persist(new Media("9782264030619", "L'Aventure ambigue")));

        inTransaction(session -> {
            sql().reset();

            Media premier = session.byNaturalId(Media.class).using("isbn", "9782264030619").load();
            assertThat(premier.getTitle()).isEqualTo("L'Aventure ambigue");
            int apresPremiere = sql().selectCount();

            Media second = session.byNaturalId(Media.class).using("isbn", "9782264030619").load();

            assertThat(second).isSameAs(premier);
            assertThat(sql().selectCount()).isEqualTo(apresPremiere); // aucune requete de plus
        });
    }

    @Test
    @DisplayName("@DynamicUpdate n'ecrit que les colonnes reellement modifiees")
    void dynamicupdate_n_ecrit_que_les_colonnes_modifiees() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("9782266114781", "Les Bouts de bois de Dieu");
            media.setSummary("Greve des cheminots du Dakar-Niger");
            media.setPageCount(379);
            session.persist(media);
            return media.getId();
        });

        inTransaction(session -> {
            Media media = session.find(Media.class, id);
            sql().reset();

            media.setPageCount(380); // une seule propriete

            session.flush();

            assertThat(sql().updateCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("page_count");
            // Sans @DynamicUpdate, l'UPDATE listerait aussi title, summary, isbn, archived...
            assertThat(sql().lastQuery()).doesNotContain("summary", "title");
        });
    }

    @Test
    @DisplayName("Sans modification, aucun UPDATE n'est emis")
    void sans_modification_aucun_update() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("9782070368228", "Le Petit Prince");
            session.persist(media);
            return media.getId();
        });

        inTransaction(session -> {
            Media media = session.find(Media.class, id);
            sql().reset();

            media.setTitle(media.getTitle()); // meme valeur

            session.flush();
            assertThat(sql().updateCount()).isZero(); // le dirty checking compare les valeurs
        });
    }
}
