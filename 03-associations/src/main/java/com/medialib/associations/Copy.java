package com.medialib.associations;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;

/**
 * Un exemplaire physique.
 *
 * <p>Cote <strong>proprietaire</strong> de la relation avec {@link Media} : c'est cette classe
 * qui porte la cle etrangere, donc la seule dont la modification declenche un UPDATE de la
 * colonne {@code media_id}.
 *
 * <p>Le {@code fetch = LAZY} explicite n'est pas du zele : le defaut de {@code @ManyToOne} est
 * EAGER, ce qui joint systematiquement l'oeuvre a chaque chargement d'exemplaire.
 */
@Entity
public class Copy {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "copy_seq")
    @SequenceGenerator(name = "copy_seq", sequenceName = "copy_seq", allocationSize = 1)
    private Long id;

    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id")
    private Media media;

    protected Copy() {
    }

    public Copy(String barcode) {
        this.barcode = barcode;
    }

    public Long getId() {
        return id;
    }

    public String getBarcode() {
        return barcode;
    }

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }
}
