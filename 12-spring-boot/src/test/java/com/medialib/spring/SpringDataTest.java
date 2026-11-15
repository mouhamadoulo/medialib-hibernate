package com.medialib.spring;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

/**
 * Spring Data JPA : ce qu'il remplace, et par quoi.
 *
 * <p>Chaque test fait le lien avec le chapitre 06, ou la meme chose etait ecrite a la main.
 * Le SQL emis est asserte, comme partout dans ce depot — c'est la seule facon de voir que
 * Spring Data ne fait aucune magie : il ecrit du HQL a notre place.
 */
class SpringDataTest extends SpringTestBase {

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void unCatalogue() {
        videLaBase();
        transactionTemplate.executeWithoutResult(status -> {
            repository.save(new Media("Une si longue lettre", 1979));
            repository.save(new Media("L'Aventure ambigue", 1961));
            repository.save(new Media("Les Bouts de bois de Dieu", 1960));
        });
        sql.reset();
    }

    @Test
    @DisplayName("Requete derivee : le nom de la methode devient du SQL")
    void une_requete_derivee_produit_le_sql_attendu() {
        List<Media> avant1970 = repository.findByPublishedYearLessThanOrderByTitleAsc(1970);

        assertThat(avant1970).extracting(Media::getTitle)
                .containsExactly("L'Aventure ambigue", "Les Bouts de bois de Dieu");
        assertThat(sql.selectCount()).isEqualTo(1);
        assertThat(sql.lastQuery()).contains("published_year<", "order by");
    }

    @Test
    @DisplayName("@Query : exactement le HQL du chapitre 06, place sur une interface")
    void une_requete_explicite_en_hql() {
        List<Media> trouvees = repository.chercheParFragmentDeTitre("longue");

        assertThat(trouvees).extracting(Media::getTitle).containsExactly("Une si longue lettre");
        assertThat(sql.selectCount()).isEqualTo(1);
        assertThat(sql.lastQuery()).contains("like");
    }

    @Test
    @DisplayName("Specification : l'API Criteria du chapitre 06, emballee")
    void une_specification_compose_les_criteres() {
        List<Media> resultat = repository.findAll(critere("longue", 1979));

        assertThat(resultat).extracting(Media::getTitle).containsExactly("Une si longue lettre");
        assertThat(sql.lastQuery()).contains("like", "published_year=");

        // Et sans critere, la meme Specification ne filtre rien.
        assertThat(repository.findAll(critere(null, null))).hasSize(3);
    }

    @Test
    @DisplayName("Projection par interface : seules les colonnes utiles sont selectionnees")
    void une_projection_par_interface_reduit_les_colonnes() {
        List<MediaRepository.TitreSeul> titres = repository.findTitreSeulByPublishedYearNotNull();

        assertThat(titres).hasSize(3);
        assertThat(titres).extracting(MediaRepository.TitreSeul::getTitle)
                .contains("Une si longue lettre");
        assertThat(sql.lastQuery()).contains("title", "published_year");
    }

    @Test
    @DisplayName("@EntityGraph de Spring Data : l'equivalent du chapitre 07")
    void un_entitygraph_evite_le_n_plus_un() {
        transactionTemplate.executeWithoutResult(status -> {
            Media media = repository.findByTitle("Une si longue lettre").orElseThrow();
            media.addCopy(new Copy("EX-0001"));
            repository.save(media);
        });
        sql.reset();

        transactionTemplate.executeWithoutResult(status -> {
            List<Media> toutes = repository.findAllByPublishedYearNotNull();
            toutes.forEach(m -> m.getCopies().size());

            // Une seule requete, avec jointure : aucun N+1.
            assertThat(sql.selectCount()).isEqualTo(1);
            assertThat(sql.lastQuery()).contains("join");
        });
    }

    @Test
    @DisplayName("L'EntityManager reste injectable, et c'est la MEME SessionFactory")
    void l_entitymanager_reste_accessible_sous_les_repositories() {
        assertThat(entityManager).isNotNull();

        // Spring Data n'est qu'une couche : dessous, c'est l'Hibernate des onze chapitres.
        Session session = entityManager.unwrap(Session.class);
        assertThat(session).isNotNull();

        SessionFactory factory = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class);
        assertThat(factory.isOpen()).isTrue();

        // Et on peut ecrire du HQL directement, sans passer par un repository.
        Long compte = transactionTemplate.execute(status -> entityManager
                .createQuery("select count(m) from Media m", Long.class).getSingleResult());
        assertThat(compte).isEqualTo(3L);
    }

    private static Specification<Media> critere(String fragment, Integer annee) {
        return (root, query, cb) -> {
            List<Predicate> criteres = new ArrayList<>();
            if (fragment != null) {
                criteres.add(cb.like(root.get("title"), "%" + fragment + "%"));
            }
            if (annee != null) {
                criteres.add(cb.equal(root.get("publishedYear"), annee));
            }
            return cb.and(criteres.toArray(new Predicate[0]));
        };
    }
}
