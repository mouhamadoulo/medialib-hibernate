package com.medialib.queries;

/**
 * Projection : exactement les deux colonnes dont l'appelant a besoin.
 *
 * <p>Un record Java 21 fait un DTO de projection parfait — immuable, un constructeur canonique
 * qu'Hibernate sait appeler depuis un {@code select new}, et aucune entite managee a la cle.
 * C'est souvent la vraie reponse au N+1, bien avant le {@code join fetch}.
 */
public record MediaResume(String title, Integer publishedYear) {
}
