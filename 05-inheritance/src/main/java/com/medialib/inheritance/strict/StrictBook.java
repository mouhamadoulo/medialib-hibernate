package com.medialib.inheritance.strict;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Un livre dont le nombre de pages est declare <strong>obligatoire</strong>.
 *
 * <p>On s'attend souvent a ce qu'Hibernate ignore cette contrainte, puisque la table est
 * partagee avec des DVD qui n'ont pas de pages. Il n'en fait rien. Voir
 * {@code NotNullEnSingleTableTest}.
 */
@Entity
@DiscriminatorValue("BOOK")
public class StrictBook extends StrictMedia {

    @Column(nullable = false)
    private Integer pageCount;

    protected StrictBook() {
    }

    public StrictBook(Long id, String title, Integer pageCount) {
        super(id, title);
        this.pageCount = pageCount;
    }

    public Integer getPageCount() {
        return pageCount;
    }
}
