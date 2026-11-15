package com.medialib.mapping;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un editeur.
 *
 * <p>Identifiant en {@code IDENTITY} : c'est la base qui l'attribue, au moment de l'insertion.
 * Hibernate est donc oblige d'emettre l'INSERT des le {@code persist()} pour connaitre la cle
 * — ce qui lui interdit de differer et de regrouper les ecritures. Voir {@code IdentifiantsTest}.
 */
@Entity
@Table(name = "publisher")
public class Publisher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    protected Publisher() {
        // requis par Hibernate
    }

    public Publisher(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
