package com.medialib.inheritance.single;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Un livre. Son nombre de pages ne peut pas etre NOT NULL : un DVD n'en a pas. */
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
