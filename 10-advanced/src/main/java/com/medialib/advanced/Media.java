package com.medialib.advanced;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import java.time.Instant;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Une oeuvre du catalogue.
 *
 * <p>Concentre quatre mecanismes qu'on croise dans toute application reelle :
 *
 * <ul>
 *   <li><strong>Soft delete</strong> ({@code @SQLDelete} + {@code @SQLRestriction}) : une
 *       mediatheque ne supprime pas une oeuvre retiree du catalogue, elle l'archive. Les
 *       emprunts passes doivent continuer de la designer.</li>
 *   <li><strong>{@code @NaturalId}</strong> : l'ISBN identifie l'ouvrage pour les humains.
 *       Hibernate lui donne son propre cache de resolution.</li>
 *   <li><strong>{@code @DynamicUpdate}</strong> : n'ecrire que les colonnes modifiees.</li>
 *   <li>Un champ d'audit rempli par un <strong>listener d'evenements</strong>, sans qu'aucun
 *       code appelant ne s'en occupe.</li>
 * </ul>
 */
@Entity
@DynamicUpdate
@SQLDelete(sql = "update media set archived = true where id = ?")
@SQLRestriction("archived = false")
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_seq")
    @SequenceGenerator(name = "media_seq", sequenceName = "media_seq", allocationSize = 1)
    private Long id;

    @NaturalId
    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    private String title;

    private String summary;

    private Integer pageCount;

    @Column(nullable = false)
    private boolean archived = false;

    /** Rempli par {@link HorodatageListener} : aucun appelant ne le renseigne. */
    private Instant createdAt;

    protected Media() {
    }

    public Media(String isbn, String title) {
        this.isbn = isbn;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public boolean isArchived() {
        return archived;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
