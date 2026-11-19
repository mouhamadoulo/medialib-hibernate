package com.medialib.caching;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** Un auteur, cache en lecture-ecriture. */
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "author_seq")
    @SequenceGenerator(name = "author_seq", sequenceName = "author_seq", allocationSize = 1)
    private Long id;

    private String name;

    protected Author() {
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

    /**
     * Permet de comparer READ_WRITE a NONSTRICT_READ_WRITE sur une modification.
     *
     * <p>Voir {@code NonStrictEtCacheModeTest} : apres cette modification, le cache sert la
     * nouvelle valeur sans retour en base — la ou {@link Publisher}, en
     * {@code NONSTRICT_READ_WRITE}, aurait vu son entree evincee.
     */
    public void setName(String name) {
        this.name = name;
    }
}
