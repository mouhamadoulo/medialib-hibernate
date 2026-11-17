package com.medialib.mapping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * Fiche documentaire d'une oeuvre : le pretexte a reunir quatre annotations de colonne qui se
 * ressemblent et n'agissent pas du tout au meme moment.
 *
 * <ul>
 *   <li>{@code @Lob} agit a la <strong>generation du schema</strong> : il choisit un type SQL
 *       dedie ({@code text} sur PostgreSQL) au lieu d'un {@code varchar} borne.
 *   <li>{@code @ColumnDefault} agit <strong>uniquement</strong> a la generation du schema. Il
 *       n'influence pas une seule insertion, parce qu'Hibernate liste toutes les colonnes dans
 *       son INSERT — un champ Java a {@code null} envoie donc un NULL explicite, et un defaut
 *       de colonne ne s'applique qu'en l'absence de valeur. C'est le piege du chapitre.
 *   <li>{@code @Check} agit aussi au seul DDL, mais il produit une contrainte que la base fera
 *       respecter. Hibernate, lui, ne verifie rien : l'INSERT part, et PostgreSQL le refuse.
 *   <li>{@code @Generated} est le seul des quatre qui change le SQL de chaque insertion : la
 *       colonne est retiree de l'INSERT, puis relue par un SELECT.
 * </ul>
 */
@Entity
@Table(name = "media_sheet")
@Check(name = "ck_media_sheet_page_count", constraints = "page_count > 0")
public class MediaSheet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_sheet_seq")
    private Long id;

    @Column(nullable = false)
    private String title;

    /** Texte long : {@code text} en base, aucune longueur a declarer. */
    @Lob
    @Column(name = "full_text")
    private String fullText;

    /**
     * Le defaut est ecrit dans le schema et n'est jamais applique par Hibernate. Voir
     * {@code ColonnesEtContraintesTest}.
     */
    @ColumnDefault("'fr'")
    @Column(length = 5)
    private String language;

    /** Soumis a la contrainte {@code @Check} declaree sur la classe. */
    @Column(name = "page_count")
    private Integer pageCount;

    /**
     * Calculee par la base a l'insertion, puis relue par Hibernate.
     *
     * <p>C'est la combinaison qui fait fonctionner un defaut de colonne : {@code @Generated}
     * retire la colonne de l'INSERT, ce qui laisse enfin le {@code @ColumnDefault} s'appliquer,
     * et le SELECT qui suit remet l'objet en memoire d'accord avec la ligne.
     */
    @Generated(event = EventType.INSERT)
    @ColumnDefault("current_date")
    @Column(name = "indexed_on", insertable = false, updatable = false)
    private LocalDate indexedOn;

    protected MediaSheet() {
        // requis par Hibernate
    }

    public MediaSheet(String title, String fullText, String language, Integer pageCount) {
        this.title = title;
        this.fullText = fullText;
        this.language = language;
        this.pageCount = pageCount;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getFullText() {
        return fullText;
    }

    public String getLanguage() {
        return language;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public LocalDate getIndexedOn() {
        return indexedOn;
    }
}
