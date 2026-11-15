package com.medialib.caching;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Une agence du reseau.
 *
 * <p>Cachee en {@code READ_ONLY} : la liste des agences change une fois par an. C'est le cas
 * ideal du cache de second niveau — beaucoup de lectures, aucune ecriture. La strategie est
 * aussi la plus rapide, puisqu'Hibernate n'a rien a invalider.
 *
 * <p>Contrepartie absolue : toute tentative de modification leve une exception. Ce n'est pas
 * un effet de bord, c'est le contrat.
 */
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "branch_seq")
    @SequenceGenerator(name = "branch_seq", sequenceName = "branch_seq", allocationSize = 1)
    private Long id;

    private String name;

    protected Branch() {
    }

    public Branch(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
