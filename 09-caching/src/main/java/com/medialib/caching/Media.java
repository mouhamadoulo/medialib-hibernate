package com.medialib.caching;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Une oeuvre du catalogue, cachee en {@code READ_WRITE}.
 *
 * <p>C'est la strategie a utiliser par defaut des qu'il y a des ecritures. Hibernate pose un
 * verrou logiciel sur l'entree de cache pendant la transaction, et l'invalide au commit. Plus
 * couteuse que {@code READ_ONLY}, mais correcte en presence de modifications.
 *
 * <p><strong>Le cache de la collection se declare separement.</strong> Mettre {@code @Cache}
 * sur l'entite ne cache pas ses collections : il faut annoter chaque association. C'est l'un
 * des oublis les plus frequents, et il se voit immediatement dans le SQL.
 */
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_seq")
    @SequenceGenerator(name = "media_seq", sequenceName = "media_seq", allocationSize = 1)
    private Long id;

    private String title;

    private Integer loanCount = 0;

    /** Collection <strong>cachee</strong> : annotation propre, distincte de celle de l'entite. */
    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "media_author",
            joinColumns = @JoinColumn(name = "media_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Author> authors = new HashSet<>();

    /** Collection <strong>non cachee</strong> : le temoin de comparaison. */
    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "media_translator",
            joinColumns = @JoinColumn(name = "media_id"),
            inverseJoinColumns = @JoinColumn(name = "translator_id"))
    private Set<Author> translators = new HashSet<>();

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

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getLoanCount() {
        return loanCount;
    }

    public Set<Author> getAuthors() {
        return authors;
    }

    public Set<Author> getTranslators() {
        return translators;
    }
}
