package com.medialib.queries;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Tuple;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HQL : jointures, agregats, sous-requetes. Tout en une requete, et on le verifie. */
class HqlTest extends RequetesTestBase {

    @Test
    @DisplayName("Une jointure explicite ne produit qu'une requete")
    void une_jointure_explicite_ne_produit_qu_une_requete() {
        unCatalogue();

        inTransaction(session -> {
            sql().reset();

            List<String> titres = session.createQuery(
                    "select m.title from Loan l join l.media m where l.member.city = :ville",
                    String.class)
                    .setParameter("ville", "Dakar")
                    .getResultList();

            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(titres).containsExactlyInAnyOrder(
                    "Une si longue lettre", "L'Aventure ambigue");
        });
    }

    @Test
    @DisplayName("Un agregat avec group by et having reste une seule requete")
    void un_agregat_avec_group_by_reste_une_requete() {
        unCatalogue();

        inTransaction(session -> {
            sql().reset();

            List<Tuple> parOeuvre = session.createQuery(
                    "select m.title, count(l) from Loan l join l.media m "
                            + "group by m.title having count(l) > 1", Tuple.class)
                    .getResultList();

            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("group by", "having");
            assertThat(parOeuvre).hasSize(1);
            assertThat(parOeuvre.get(0).get(0)).isEqualTo("Une si longue lettre");
            assertThat(parOeuvre.get(0).get(1)).isEqualTo(2L);
        });
    }

    @Test
    @DisplayName("Une sous-requete correlee reste une seule requete")
    void une_sous_requete_correlee_reste_une_seule_requete() {
        unCatalogue();

        inTransaction(session -> {
            sql().reset();

            List<Media> jamaisEmpruntes = session.createQuery(
                    "select m from Media m where not exists "
                            + "(select l from Loan l where l.media = m)", Media.class)
                    .getResultList();

            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("exists");
            assertThat(jamaisEmpruntes).extracting(Media::getTitle)
                    .containsExactly("Les Bouts de bois de Dieu");
        });
    }

    @Test
    @DisplayName("left join : les oeuvres sans emprunt figurent quand meme")
    void un_left_join_conserve_les_lignes_sans_correspondance() {
        unCatalogue();

        inTransaction(session -> {
            sql().reset();

            List<Tuple> comptes = session.createQuery(
                    "select m.title, count(l) from Media m left join Loan l on l.media = m "
                            + "group by m.title order by m.title", Tuple.class)
                    .getResultList();

            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("left join");
            assertThat(comptes).hasSize(3); // les trois oeuvres, dont celle a zero emprunt
        });
    }

    @Test
    @DisplayName("case et coalesce sont evalues par la base")
    void case_et_coalesce_sont_evalues_en_sql() {
        unCatalogue();

        inTransaction(session -> {
            sql().reset();

            List<String> etats = session.createQuery(
                    "select case when l.returnedOn is null then 'en cours' else 'rendu' end "
                            + "from Loan l", String.class)
                    .getResultList();

            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("case when");
            assertThat(etats).containsOnly("en cours");
        });
    }
}
