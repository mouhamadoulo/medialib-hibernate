package com.medialib.performance;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Un mot-cle du catalogue.
 *
 * <p>Identifiant en {@code IDENTITY}, et c'est tout le propos : la base attribue la cle au
 * moment de l'INSERT, donc Hibernate doit emettre chaque INSERT separement pour la recuperer.
 * Le batching est <strong>desactive en silence</strong>, quel que soit
 * {@code hibernate.jdbc.batch_size}. Voir {@code BatchingTest}.
 */
@Entity
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String label;

    protected Tag() {
    }

    public Tag(String label) {
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
