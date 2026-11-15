package com.medialib.inheritance.single;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Hierarchie en {@code SINGLE_TABLE} : tout tient dans une seule table.
 *
 * <p>Une colonne discriminante dit de quel type est chaque ligne. Les colonnes propres aux
 * sous-classes cohabitent, donc <strong>aucune ne peut etre NOT NULL</strong> : un vinyle n'a
 * pas de nombre de pages. C'est le prix a payer, et il est structurel — la base ne peut plus
 * garantir l'integrite que le modele Java exprime.
 *
 * <p>En echange : une seule table, jamais de jointure, les requetes polymorphes les plus
 * rapides des trois strategies.
 */
@Entity
@Table(name = "single_media")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "media_type", discriminatorType = DiscriminatorType.STRING, length = 10)
public abstract class SingleMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "single_media_seq")
    @SequenceGenerator(name = "single_media_seq", sequenceName = "single_media_seq",
            allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String title;

    protected SingleMedia() {
    }

    protected SingleMedia(String title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
