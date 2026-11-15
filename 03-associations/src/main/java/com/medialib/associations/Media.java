package com.medialib.associations;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Une oeuvre du catalogue.
 *
 * <p>Porte les deux associations les plus frequentes, et les plus mal comprises :
 * un {@code @OneToMany} <strong>bidirectionnel</strong> vers ses exemplaires, et un
 * {@code @ManyToMany} vers ses auteurs.
 */
@Entity
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_seq")
    @SequenceGenerator(name = "media_seq", sequenceName = "media_seq", allocationSize = 1)
    private Long id;

    private String title;

    /**
     * Bidirectionnel : {@code mappedBy} designe {@code Copy.media} comme cote proprietaire.
     * C'est donc {@code Copy} qui ecrit la cle etrangere, et cette collection n'est qu'une vue.
     *
     * <p>{@code orphanRemoval} : un exemplaire retire de cette liste n'a plus de raison
     * d'exister — un exemplaire sans oeuvre n'est rien. C'est exactement le critere qui
     * distingue {@code orphanRemoval} d'un simple {@code CascadeType.REMOVE}.
     */
    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Copy> copies = new ArrayList<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "media_author",
            joinColumns = @JoinColumn(name = "media_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id"))
    private Set<Author> authors = new HashSet<>();

    protected Media() {
    }

    public Media(String title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<Copy> getCopies() {
        return copies;
    }

    public Set<Author> getAuthors() {
        return authors;
    }

    /**
     * Methode utilitaire de synchronisation : elle met a jour <strong>les deux</strong> cotes.
     * Sans elle, ajouter a la collection ne suffit pas — c'est {@code Copy.media} qui ecrit la
     * cle etrangere. Voir {@code OneToManyTest}.
     */
    public void addCopy(Copy copy) {
        copies.add(copy);
        copy.setMedia(this);
    }

    public void removeCopy(Copy copy) {
        copies.remove(copy);
        copy.setMedia(null);
    }

    public void addAuthor(Author author) {
        authors.add(author);
    }
}
