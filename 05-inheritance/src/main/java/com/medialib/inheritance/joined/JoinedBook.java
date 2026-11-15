package com.medialib.inheritance.joined;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Un livre.
 *
 * <p>Ici, {@code page_count} est <strong>NOT NULL</strong> : la contrainte est possible parce
 * que la colonne ne concerne que les livres. C'est l'avantage decisif de {@code JOINED}.
 */
@Entity
@Table(name = "joined_book")
@PrimaryKeyJoinColumn(name = "media_id")
public class JoinedBook extends JoinedMedia {

    private String isbn;

    @Column(nullable = false)
    private Integer pageCount;

    protected JoinedBook() {
    }

    public JoinedBook(String title, String isbn, Integer pageCount) {
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
