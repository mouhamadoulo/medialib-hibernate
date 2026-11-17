package com.medialib.mapping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un auteur, identifie par le generateur {@code AUTO} — c'est-a-dire par celui que le dialecte
 * juge le meilleur.
 *
 * <p>{@code AUTO} est lu de travers a peu pres partout : on l'entend comme « laisse la base
 * decider », donc comme une colonne auto-incrementee. Sur PostgreSQL avec Hibernate 7, il se
 * resout en {@code SEQUENCE}, avec une sequence par entite nommee d'apres la table
 * ({@code author_seq}).
 *
 * <p>La difference est tout sauf cosmetique : {@code IDENTITY} obligerait Hibernate a inserer
 * des le {@code persist} pour connaitre la cle, ce qui interdit tout regroupement d'insertions
 * (voir le chapitre 11). {@code SEQUENCE} lui laisse differer jusqu'au flush. Et comme
 * l'{@code allocationSize} par defaut vaut 50, un seul appel a la sequence sert cinquante
 * identifiants.
 *
 * <p>Corollaire : la meme annotation ne produit pas le meme schema d'un dialecte a l'autre.
 * Sur MySQL, qui n'a pas de sequences, {@code AUTO} retombe sur autre chose.
 */
@Entity
@Table(name = "author")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    protected Author() {
        // requis par Hibernate
    }

    public Author(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
