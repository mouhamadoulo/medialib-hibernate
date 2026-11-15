package com.medialib.queries;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * L'API Criteria, et la raison pour laquelle on l'utilise : les requetes construites a
 * l'execution, dont on ne connait pas les criteres a l'avance (un formulaire de recherche).
 *
 * <p>Le metamodele statique ({@code Media_.title}) est genere a la compilation par
 * {@code hibernate-jpamodelgen}. Sans lui, on ecrirait {@code root.get("title")} — une chaine
 * de caracteres qu'aucun compilateur ne verifie, et qui casse silencieusement au moindre
 * renommage d'attribut.
 */
class CriteriaTest extends RequetesTestBase {

    @Test
    @DisplayName("Le metamodele statique est genere et typé a la compilation")
    void le_metamodele_statique_est_genere() {
        // Si hibernate-jpamodelgen n'avait pas tourne, cette ligne ne compilerait meme pas.
        assertThat(Media_.title.getName()).isEqualTo("title");
        assertThat(Media_.publishedYear.getName()).isEqualTo("publishedYear");
        assertThat(Author_.country.getName()).isEqualTo("country");
    }

    @Test
    @DisplayName("Une Criteria produit le meme SQL que le HQL equivalent")
    void une_criteria_produit_le_meme_sql_que_le_hql_equivalent() {
        unCatalogue();

        String sqlDuHql = inTransactionReturning(session -> {
            sql().reset();
            session.createQuery(
                    "select m from Media m where m.publishedYear < :annee order by m.title",
                    Media.class)
                    .setParameter("annee", 1970)
                    .getResultList();
            return sql().lastQuery();
        });

        String sqlDeLaCriteria = inTransactionReturning(session -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Media> query = cb.createQuery(Media.class);
            Root<Media> media = query.from(Media.class);
            query.select(media)
                    .where(cb.lessThan(media.get(Media_.publishedYear), 1970))
                    .orderBy(cb.asc(media.get(Media_.title)));

            sql().reset();
            session.createQuery(query).getResultList();
            return sql().lastQuery();
        });

        assertThat(sqlDeLaCriteria).isEqualTo(sqlDuHql);
    }

    @Test
    @DisplayName("Criteria compose les criteres a l'execution : c'est son seul vrai interet")
    void une_criteria_compose_des_criteres_optionnels() {
        unCatalogue();

        // Un formulaire de recherche : les deux criteres sont facultatifs.
        List<Media> avecLesDeux = recherche("lettre", 1979);
        List<Media> titreSeul = recherche("Bouts", null);
        List<Media> sansCritere = recherche(null, null);

        assertThat(avecLesDeux).extracting(Media::getTitle)
                .containsExactly("Une si longue lettre");
        assertThat(titreSeul).extracting(Media::getTitle)
                .containsExactly("Les Bouts de bois de Dieu");
        assertThat(sansCritere).hasSize(3);
    }

    private List<Media> recherche(String fragmentDeTitre, Integer annee) {
        return inTransactionReturning(session -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Media> query = cb.createQuery(Media.class);
            Root<Media> media = query.from(Media.class);

            List<jakarta.persistence.criteria.Predicate> criteres = new java.util.ArrayList<>();
            if (fragmentDeTitre != null) {
                criteres.add(cb.like(media.get(Media_.title), "%" + fragmentDeTitre + "%"));
            }
            if (annee != null) {
                criteres.add(cb.equal(media.get(Media_.publishedYear), annee));
            }

            query.select(media).where(cb.and(criteres.toArray(
                    new jakarta.persistence.criteria.Predicate[0])));
            return session.createQuery(query).getResultList();
        });
    }
}
