package com.medialib.queries;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pagination, et la faille que la concatenation de chaines ouvre dans une requete. */
class PaginationEtInjectionTest extends RequetesTestBase {

    @Test
    @DisplayName("setMaxResults et setFirstResult ajoutent offset et fetch first au SQL")
    void setmaxresults_ajoute_limit_et_offset() {
        unCatalogue();

        inTransaction(session -> {
            sql().reset();

            List<Media> page = session.createQuery(
                    "select m from Media m order by m.title", Media.class)
                    .setFirstResult(1)
                    .setMaxResults(1)
                    .getResultList();

            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("offset", "fetch first");
            assertThat(page).extracting(Media::getTitle)
                    .containsExactly("Les Bouts de bois de Dieu"); // 2e par ordre alphabetique
        });
    }

    @Test
    @DisplayName("Une pagination sans order by rend un resultat non deterministe")
    void la_pagination_exige_un_tri_explicite() {
        unCatalogue();

        inTransaction(session -> {
            sql().reset();

            session.createQuery("select m from Media m", Media.class)
                    .setMaxResults(1)
                    .getResultList();

            // La base est libre de rendre n'importe quelle ligne : rien ne fixe l'ordre.
            // Le SQL le montre — un fetch first sans order by.
            assertThat(sql().lastQuery()).contains("fetch first");
            assertThat(sql().lastQuery()).doesNotContain("order by");
        });
    }

    @Test
    @DisplayName("La concatenation ouvre une injection : la clause where est detournee")
    void la_concatenation_ouvre_une_injection() {
        unCatalogue();

        // Valeur fournie par un utilisateur mal intentionne.
        String saisie = "Inconnu' or '1'='1";

        inTransaction(session -> {
            @SuppressWarnings("deprecation")
            List<?> resultats = session.createNativeQuery(
                    "select title from media where title = '" + saisie + "'")
                    .getResultList();

            // On cherchait une oeuvre inexistante : on recupere tout le catalogue.
            assertThat(resultats).hasSize(3);
        });
    }

    @Test
    @DisplayName("Un parametre nomme ferme la faille : la valeur reste une donnee")
    void un_parametre_nomme_la_ferme() {
        unCatalogue();

        String saisie = "Inconnu' or '1'='1";

        inTransaction(session -> {
            sql().reset();

            List<Media> resultats = session.createQuery(
                    "select m from Media m where m.title = :titre", Media.class)
                    .setParameter("titre", saisie)
                    .getResultList();

            assertThat(resultats).isEmpty(); // traitee comme un titre, pas comme du SQL
            assertThat(sql().lastQuery()).contains("=?");
            assertThat(sql().lastQuery()).doesNotContain("1'='1");
        });
    }
}
