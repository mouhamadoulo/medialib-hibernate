package com.medialib.fetching;

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
 * <p>Deux {@code @ManyToOne} volontairement differents :
 * <ul>
 *   <li>{@code media} est explicitement LAZY — on ne la charge que si on y touche ;</li>
 *   <li>{@code branch} laisse le defaut, donc EAGER. Chaque chargement d'exemplaire joint
 *       l'agence, qu'on en ait besoin ou non. C'est le defaut le plus couteux de JPA, et le
 *       plus facile a oublier.</li>
 * </ul>
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

    /** Aucun fetch declare : @ManyToOne est EAGER par defaut. */
    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    protected Copy() {
    }

    public Copy(String barcode, Branch branch) {
        this.barcode = barcode;
        this.branch = branch;
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

    public Branch getBranch() {
        return branch;
    }
}
