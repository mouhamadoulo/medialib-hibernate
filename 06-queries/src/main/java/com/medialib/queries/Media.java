package com.medialib.queries;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SqlResultSetMapping;
import java.util.HashSet;
import java.util.Set;

/**
 * Une oeuvre du catalogue.
 *
 * <p>Porte les requetes nommees du chapitre. Leur interet n'est pas la concision : c'est que
 * <strong>Hibernate les valide au demarrage</strong>. Une requete nommee fautive fait echouer
 * la construction de la {@code SessionFactory}, pas la premiere requete d'un utilisateur en
 * production.
 */
@Entity
@NamedQuery(name = "Media.parAnnee",
        query = "select m from Media m where m.publishedYear = :annee order by m.title")
@NamedQuery(name = "Media.resumes",
        query = "select new com.medialib.queries.MediaResume(m.title, m.publishedYear) from Media m")
@NamedNativeQuery(name = "Media.resumesNatifs",
        query = "select title, published_year from media order by title",
        resultSetMapping = "MediaResumeMapping")
@SqlResultSetMapping(name = "MediaResumeMapping",
        classes = @ConstructorResult(targetClass = MediaResume.class,
                columns = {
                    @ColumnResult(name = "title", type = String.class),
                    @ColumnResult(name = "published_year", type = Integer.class)
                }))
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_seq")
    @SequenceGenerator(name = "media_seq", sequenceName = "media_seq", allocationSize = 1)
    private Long id;

    private String title;

    private Integer publishedYear;

    @ManyToMany
    @JoinTable(name = "media_author",
            joinColumns = @JoinColumn(name = "media_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id"))
    private Set<Author> authors = new HashSet<>();

    protected Media() {
    }

    public Media(String title, Integer publishedYear) {
        this.title = title;
        this.publishedYear = publishedYear;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Integer getPublishedYear() {
        return publishedYear;
    }

    public Set<Author> getAuthors() {
        return authors;
    }

    public void addAuthor(Author author) {
        authors.add(author);
        author.getMedias().add(this);
    }
}
