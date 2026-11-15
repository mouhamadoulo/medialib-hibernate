package com.medialib.inheritance.joined;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Hierarchie en {@code JOINED} : une table par classe, reliees par la cle primaire.
 *
 * <p>Le modele relationnel est propre : chaque colonne vit dans la table ou elle a du sens, et
 * {@code page_count} peut etre NOT NULL sans gener les vinyles. En echange, charger un livre
 * exige une jointure, et une requete polymorphe en exige autant qu'il y a de sous-classes.
 */
@Entity
@Table(name = "joined_media")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class JoinedMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "joined_media_seq")
    @SequenceGenerator(name = "joined_media_seq", sequenceName = "joined_media_seq",
            allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String title;

    protected JoinedMedia() {
    }

    protected JoinedMedia(String title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
