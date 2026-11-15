package com.medialib.inheritance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.medialib.inheritance.joined.JoinedMedia;
import com.medialib.inheritance.mapped.AbstractAuditable;
import com.medialib.inheritance.mapped.Equipment;
import com.medialib.inheritance.single.SingleMedia;
import com.medialib.inheritance.tpc.TpcMedia;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La meme requete polymorphe — « donne-moi tout le catalogue » — selon les trois strategies.
 *
 * <p>Seuls le nombre et la forme des requetes sont assertes. Les durees sont journalisees, pas
 * assertees : une assertion temporelle serait instable chez quiconque clone ce depot.
 */
class ComparatifTest extends HeritageTestBase {

    @Test
    @DisplayName("SINGLE_TABLE : une requete, une table, aucune jointure")
    void requete_polymorphe_en_single_table() {
        catalogueDansLesTroisStrategies();
        rechauffe();

        inTransaction(session -> {
            sql().reset();
            long debut = System.nanoTime();

            List<SingleMedia> catalogue = session
                    .createQuery("select m from SingleMedia m", SingleMedia.class).getResultList();

            journalise("SINGLE_TABLE", debut);
            assertThat(catalogue).hasSize(3);
            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).doesNotContain("join", "union");
        });
    }

    @Test
    @DisplayName("JOINED : une requete, mais une jointure externe par sous-classe")
    void requete_polymorphe_en_joined() {
        catalogueDansLesTroisStrategies();
        rechauffe();

        inTransaction(session -> {
            sql().reset();
            long debut = System.nanoTime();

            List<JoinedMedia> catalogue = session
                    .createQuery("select m from JoinedMedia m", JoinedMedia.class).getResultList();

            journalise("JOINED", debut);
            assertThat(catalogue).hasSize(3);
            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("left join");
        });
    }

    @Test
    @DisplayName("TABLE_PER_CLASS : une requete, mais un UNION ALL sur toutes les tables")
    void une_requete_polymorphe_genere_un_union_all() {
        catalogueDansLesTroisStrategies();
        rechauffe();

        inTransaction(session -> {
            sql().reset();
            long debut = System.nanoTime();

            List<TpcMedia> catalogue = session
                    .createQuery("select m from TpcMedia m", TpcMedia.class).getResultList();

            journalise("TABLE_PER_CLASS", debut);
            assertThat(catalogue).hasSize(3);
            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("union all");
        });
    }

    @Test
    @DisplayName("TABLE_PER_CLASS : la table de la racine n'est meme pas creee")
    void table_per_class_n_a_pas_de_table_parent_referencable() {
        catalogueDansLesTroisStrategies();

        Long tableParent = inTransactionReturning(session -> session.createNativeQuery(
                "select count(*) from information_schema.tables "
                        + "where table_schema = current_schema() and table_name = 'tpc_media'",
                Long.class).getSingleResult());

        // Hibernate ne cree aucune table pour la racine abstraite : chaque media vit
        // uniquement dans la table de sa sous-classe. Consequence directe et souvent
        // decouverte trop tard — aucune cle etrangere ne peut designer « un media ».
        assertThat(tableParent).isZero();

        // Les trois sous-classes, elles, ont bien leur table et leur ligne.
        for (String table : List.of("tpc_book", "tpc_dvd", "tpc_vinyl")) {
            Long lignes = inTransactionReturning(session -> session
                    .createNativeQuery("select count(*) from " + table, Long.class)
                    .getSingleResult());
            assertThat(lignes).as(table).isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("JOINED, a l'inverse, cree bien la table de la racine : elle est referencable")
    void joined_cree_la_table_de_la_racine() {
        Long tableParent = inTransactionReturning(session -> session.createNativeQuery(
                "select count(*) from information_schema.tables "
                        + "where table_schema = current_schema() and table_name = 'joined_media'",
                Long.class).getSingleResult());

        assertThat(tableParent).isEqualTo(1L);
    }

    @Test
    @DisplayName("type() filtre sur le discriminant sans charger les sous-classes")
    void type_filtre_sur_le_discriminant() {
        catalogueDansLesTroisStrategies();

        inTransaction(session -> {
            sql().reset();

            List<SingleMedia> livres = session.createQuery(
                    "select m from SingleMedia m where type(m) = SingleBook", SingleMedia.class)
                    .getResultList();

            assertThat(livres).hasSize(1);
            assertThat(sql().lastQuery()).contains("media_type");
        });
    }

    @Test
    @DisplayName("@MappedSuperclass n'est pas une entite : on ne peut pas l'interroger")
    void une_mappedsuperclass_n_est_pas_interrogeable() {
        inTransaction(session ->
                session.persist(new Equipment("Tourne-disque", Instant.now(), "awa")));

        inTransaction(session -> assertThatThrownBy(() -> session
                .createQuery("select a from AbstractAuditable a", AbstractAuditable.class)
                .getResultList())
                .hasMessageContaining("AbstractAuditable"));
    }

    @Test
    @DisplayName("@MappedSuperclass : ses colonnes sont recopiees dans la table de l'entite")
    void une_mappedsuperclass_recopie_ses_colonnes() {
        inTransaction(session -> {
            sql().reset();
            session.persist(new Equipment("Liseuse", Instant.now(), "awa"));
            session.flush();

            assertThat(sql().insertCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("insert into equipment", "created_at", "created_by");
        });
    }

    /**
     * Prechauffage obligatoire avant toute mesure.
     *
     * <p>Sans lui, la premiere strategie mesuree porte seule le cout de la compilation HQL, de
     * la preparation JDBC et du premier plan d'execution PostgreSQL — on observait un facteur
     * cent entre la premiere strategie et les deux suivantes, qui ne disait rien des
     * strategies elles-memes. C'est la raison pour laquelle ce depot n'assertionne jamais un
     * temps : meme journalise, un chiffre demande des precautions.
     */
    private void rechauffe() {
        inTransaction(session -> {
            session.createQuery("select m from SingleMedia m", SingleMedia.class).getResultList();
            session.createQuery("select m from JoinedMedia m", JoinedMedia.class).getResultList();
            session.createQuery("select m from TpcMedia m", TpcMedia.class).getResultList();
        });
    }

    private static void journalise(String strategie, long debutNanos) {
        long micros = (System.nanoTime() - debutNanos) / 1_000;
        System.out.printf("[comparatif] requete polymorphe %-16s : %6d us (indicatif, jamais asserte)%n",
                strategie, micros);
    }
}
