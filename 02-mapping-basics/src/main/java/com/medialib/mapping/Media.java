package com.medialib.mapping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Type;

/**
 * Une oeuvre du catalogue.
 *
 * <p>Cette entite concentre volontairement les mecanismes de mapping de valeur du chapitre :
 * un {@link UserType} ({@link Isbn}), deux mappings d'enumeration opposes, une colonne
 * calculee en SQL ({@link Formula}) et un champ ignore ({@link Transient}).
 */
@Entity
@Table(name = "media")
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_seq")
    @SequenceGenerator(name = "media_seq", sequenceName = "media_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Type(IsbnType.class)
    @Column(name = "isbn", length = 20)
    private Isbn isbn;

    /**
     * Mapping ORDINAL : la base stocke la POSITION de la constante, pas son nom. Pratique et
     * compact — et redoutable des qu'on touche a l'ordre de l'enumeration.
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "format_ordinal")
    private MediaFormat formatOrdinal;

    /** Mapping STRING : la base stocke le nom. Plus verbeux, insensible au reordonnancement. */
    @Enumerated(EnumType.STRING)
    @Column(name = "format_string", length = 20)
    private MediaFormat formatString;

    @Column(name = "page_count")
    private Integer pageCount;

    /**
     * Colonne calculee par la base a chaque chargement. Elle n'existe pas dans la table : elle
     * est injectee dans le SELECT. Donc jamais de seconde requete, mais aucune possibilite de
     * la filtrer par index.
     */
    @Formula("length(title)")
    private int titleLength;

    /** Jamais persiste : calcule en Java a la demande. */
    @Transient
    private String resume;

    protected Media() {
        // requis par Hibernate
    }

    public Media(String title, Isbn isbn, MediaFormat format, Integer pageCount) {
        this.title = title;
        this.isbn = isbn;
        this.formatOrdinal = format;
        this.formatString = format;
        this.pageCount = pageCount;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Isbn getIsbn() {
        return isbn;
    }

    public MediaFormat getFormatOrdinal() {
        return formatOrdinal;
    }

    public MediaFormat getFormatString() {
        return formatString;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public int getTitleLength() {
        return titleLength;
    }

    public String getResume() {
        return resume;
    }

    public void setResume(String resume) {
        this.resume = resume;
    }
}
