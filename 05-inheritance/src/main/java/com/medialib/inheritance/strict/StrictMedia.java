package com.medialib.inheritance.strict;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Hierarchie {@code SINGLE_TABLE} dediee a une seule demonstration : ce qui arrive quand on
 * exige {@code nullable = false} sur une colonne de sous-classe.
 *
 * <p>Elle est isolee des trois hierarchies principales du chapitre precisement parce que la
 * demonstration consiste a casser quelque chose. L'identifiant est fourni a la main pour que
 * le test n'ait a se preoccuper d'aucune sequence.
 */
@Entity
@Table(name = "strict_media")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "media_type", length = 10)
public abstract class StrictMedia {

    @Id
    private Long id;

    @Column(nullable = false)
    private String title;

    protected StrictMedia() {
    }

    protected StrictMedia(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
