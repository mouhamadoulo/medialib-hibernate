package com.medialib.fetching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le piege qui reunit les deux chapitres : paginer une requete qui joint une collection.
 *
 * <p>Joindre une collection multiplie les lignes SQL. Un {@code fetch first 10 rows} porterait
 * alors sur les lignes de la jointure, pas sur les oeuvres — on obtiendrait trois oeuvres au
 * lieu de dix. Hibernate refuse de rendre un resultat faux : il retire la pagination du SQL,
 * ramene <strong>tout</strong>, et pagine en memoire. Correct, mais catastrophique en
 * volumetrie, et annonce par un simple avertissement dans les logs.
 */
class PaginationFetchTest extends ChargementTestBase {

    @Test
    @DisplayName("join fetch sur une collection + pagination : la pagination quitte le SQL")
    void join_fetch_plus_pagination_pagine_en_memoire() {
        centOeuvresEmpruntees();

        inTransaction(session -> {
            sql().reset();

            List<Media> page = session.createQuery(
                    "select m from Media m join fetch m.copies", Media.class)
                    .setFirstResult(0)
                    .setMaxResults(10)
                    .getResultList();

            assertThat(page).hasSize(10); // le resultat est juste...

            // ...mais le SQL ne contient aucune limite : les cent oeuvres ont ete ramenees,
            // puis dix ont ete gardees cote Java.
            assertThat(sql().lastQuery()).doesNotContain("fetch first", "offset", "limit");
        });
    }

    @Test
    @DisplayName("Sans join fetch de collection, la pagination reste dans le SQL")
    void sans_collection_la_pagination_descend_en_sql() {
        centOeuvresEmpruntees();

        inTransaction(session -> {
            sql().reset();

            List<Media> page = session.createQuery("select m from Media m", Media.class)
                    .setMaxResults(10)
                    .getResultList();

            assertThat(page).hasSize(10);
            assertThat(sql().lastQuery()).contains("fetch first");
        });
    }

    @Test
    @DisplayName("La bonne solution : paginer les identifiants, puis charger ce qu'il faut")
    void paginer_les_identifiants_puis_charger() {
        centOeuvresEmpruntees();

        inTransaction(session -> {
            sql().reset();

            // Premiere requete : la page d'identifiants, paginee par la base.
            List<Long> ids = session.createQuery("select m.id from Media m order by m.id",
                    Long.class).setMaxResults(10).getResultList();
            assertThat(sql().lastQuery()).contains("fetch first");

            // Seconde requete : les dix oeuvres, collections incluses, sans pagination.
            List<Media> page = session.createQuery(
                    "select m from Media m join fetch m.copies where m.id in :ids", Media.class)
                    .setParameter("ids", ids)
                    .getResultList();

            assertThat(page).hasSize(10);
            assertThat(page).allMatch(m -> !m.getCopies().isEmpty());

            // Trois requetes, et non deux : charger les exemplaires reveille le EAGER de
            // Copy.branch, qui part en requete separee (voir LazyEagerTest). Le point du
            // test tient quand meme : les trois requetes sont bornees, aucune ne ramene
            // les cent oeuvres.
            assertThat(sql().selectCount()).isEqualTo(3);
        });
    }
}
