package com.medialib.collections;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Trier : par la base, ou en memoire ? Les deux existent, et la difference se lit dans le SQL.
 */
class TriEtMapTest extends CollectionsTestBase {

    @Test
    @DisplayName("@OrderBy fait trier la base : un ORDER BY part dans la requete")
    void orderby_trie_en_sql() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("L'Aventure ambigue");
            media.addReview(new Review(3, "correct"));
            media.addReview(new Review(5, "magistral"));
            media.addReview(new Review(4, "tres bon"));
            session.persist(media);
            return media.getId();
        });

        inTransaction(session -> {
            Media media = session.find(Media.class, id);
            sql().reset();

            media.getReviews().size(); // declenche le chargement de la collection

            assertThat(sql().lastQuery()).contains("order by");
            assertThat(media.getReviews()).extracting(Review::getRating).containsExactly(5, 4, 3);
        });
    }

    @Test
    @DisplayName("@SortNatural trie en memoire : aucun ORDER BY en SQL")
    void sortedset_trie_en_memoire() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("Les Bouts de bois de Dieu");
            media.getTags().add("roman");
            media.getTags().add("afrique");
            media.getTags().add("greve");
            session.persist(media);
            return media.getId();
        });

        inTransaction(session -> {
            Media media = session.find(Media.class, id);
            sql().reset();

            media.getTags().size();

            assertThat(sql().lastQuery()).doesNotContain("order by");
            assertThat(media.getTags()).containsExactly("afrique", "greve", "roman"); // trie quand meme
        });
    }

    @Test
    @DisplayName("@ElementCollection : une table dediee, sans entite ni identifiant")
    void les_tags_vivent_dans_une_table_dediee_sans_entite() {
        inTransaction(session -> {
            Media media = new Media("Une si longue lettre");
            media.getTags().add("roman");
            media.getTags().add("epistolaire");
            sql().reset();

            session.persist(media);
            session.flush();

            assertThat(sql().allNormalized())
                    .filteredOn(s -> s.startsWith("insert into media_tag"))
                    .hasSize(2);
        });

        // Aucune colonne d'identifiant : juste la cle du parent et la valeur.
        Long colonnes = inTransactionReturning(session -> session.createNativeQuery(
                "select count(*) from information_schema.columns where table_name = 'media_tag'",
                Long.class).getSingleResult());
        assertThat(colonnes).isEqualTo(2L);
    }

    @Test
    @DisplayName("Une Map range sa cle dans une colonne de la table de collection")
    void une_map_est_mappee_par_mapkeycolumn() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("Une si longue lettre");
            media.getTitles().put(Language.FR, "Une si longue lettre");
            media.getTitles().put(Language.EN, "So Long a Letter");
            session.persist(media);
            return media.getId();
        });

        String langue = inTransactionReturning(session -> session.createNativeQuery(
                "select language from media_title where media_id = :id and translated_title = :t",
                String.class)
                .setParameter("id", id)
                .setParameter("t", "So Long a Letter")
                .getSingleResult());

        assertThat(langue).isEqualTo("EN"); // @MapKeyEnumerated(STRING)

        inTransaction(session -> {
            Media media = session.find(Media.class, id);
            assertThat(media.getTitles()).containsEntry(Language.EN, "So Long a Letter");
        });
    }
}
