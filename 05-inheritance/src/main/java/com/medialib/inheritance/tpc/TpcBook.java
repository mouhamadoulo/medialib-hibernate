package com.medialib.inheritance.tpc;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Un livre. Sa table repete title, heritee du parent. */
@Entity
@Table(name = "tpc_book")
public class TpcBook extends TpcMedia {

    private String isbn;

    private Integer pageCount;

    protected TpcBook() {
    }

    public TpcBook(String title, String isbn, Integer pageCount) {
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
