package com.medialib.inheritance.single;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Un livre.
 *
 * <p>{@code pageCount} reste volontairement nullable : il doit cohabiter dans la meme table
 * que les DVD et les vinyles, qui n'ont pas de pages. Ce qui se passe quand on tente malgre
 * tout d'exiger {@code nullable = false} est demontre a part, sur une hierarchie dediee —
 * voir {@code strict.StrictBook} et {@code NotNullEnSingleTableTest}.
 */
@Entity
@DiscriminatorValue("BOOK")
public class SingleBook extends SingleMedia {

    private String isbn;

    private Integer pageCount;

    protected SingleBook() {
    }

    public SingleBook(String title, String isbn, Integer pageCount) {
        super(title);
        this.isbn = isbn;
        this.pageCount = pageCount;
    }

    public String getIsbn() {
        return isbn;
    }

    public Integer getPageCount() {
        return pageCount;
    }
}
