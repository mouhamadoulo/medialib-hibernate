package com.medialib.inheritance.formula;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Un livre du schema repris : c'est la presence de son {@code isbn} qui le designe comme tel.
 *
 * <p>La valeur de discriminant n'est comparee a aucune colonne — elle est comparee au resultat
 * de la formule declaree sur {@link LegacyMedia}.
 */
@Entity
@DiscriminatorValue("BOOK")
public class LegacyBook extends LegacyMedia {

    private String isbn;

    protected LegacyBook() {
    }

    public LegacyBook(Long id, String title, String isbn) {
        super(id, title);
        this.isbn = isbn;
    }

    public String getIsbn() {
        return isbn;
    }
}
