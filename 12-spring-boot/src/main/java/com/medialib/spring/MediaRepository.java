package com.medialib.spring;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Le repository Spring Data de {@link Media}.
 *
 * <p>Ce qu'il apporte reellement : la suppression du code de plomberie. Ce qu'il cache : le
 * {@code EntityManager}, le cycle de la session, le moment du flush. C'est pour cette raison
 * que ce depot ne l'introduit qu'au douzieme chapitre.
 */
public interface MediaRepository extends JpaRepository<Media, Long>, JpaSpecificationExecutor<Media> {

    /** Requete derivee : Spring Data la deduit du NOM de la methode. */
    List<Media> findByPublishedYearLessThanOrderByTitleAsc(Integer annee);

    Optional<Media> findByTitle(String titre);

    /** Requete explicite, en HQL : exactement le meme HQL qu'au chapitre 06. */
    @Query("select m from Media m where m.title like concat('%', :fragment, '%')")
    List<Media> chercheParFragmentDeTitre(@Param("fragment") String fragment);

    /** L'equivalent Spring Data du @EntityGraph du chapitre 07. */
    @EntityGraph(attributePaths = "copies")
    List<Media> findAllByPublishedYearNotNull();

    /** Projection par interface : Spring Data ne selectionne que les colonnes utiles. */
    interface TitreSeul {
        String getTitle();

        Integer getPublishedYear();
    }

    List<TitreSeul> findTitreSeulByPublishedYearNotNull();
}
