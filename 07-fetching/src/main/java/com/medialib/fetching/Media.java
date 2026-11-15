package com.medialib.fetching;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.BatchSize;

/**
 * Une oeuvre du catalogue.
 *
 * <p><strong>{@code @BatchSize(size = 10)} au niveau de la classe.</strong> L'annotation
 * s'applique alors a tous les proxies de {@code Media}, d'ou qu'ils viennent. C'est ce qui fait
 * passer le N+1 de 101 requetes a 11, sans toucher une seule ligne de code appelant. Comparer
 * avec {@link Member}, qui ne la porte pas.
 */
@Entity
@BatchSize(size = 10)
@NamedEntityGraph(name = "Media.avecAuteurs",
        attributeNodes = @NamedAttributeNode("authors"))
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_seq")
    @SequenceGenerator(name = "media_seq", sequenceName = "media_seq", allocationSize = 1)
    private Long id;

    private String title;

    /** {@code @OneToMany} est LAZY par defaut : rien n'est charge avant qu'on y touche. */
    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL)
    private List<Copy> copies = new ArrayList<>();

    /** {@code @ManyToMany} est LAZY par defaut, lui aussi. */
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
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

    public void addCopy(Copy copy) {
        copies.add(copy);
        copy.setMedia(this);
    }

    public void addAuthor(Author author) {
        authors.add(author);
    }
}
