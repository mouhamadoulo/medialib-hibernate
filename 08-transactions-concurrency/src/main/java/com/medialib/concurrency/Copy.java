package com.medialib.concurrency;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Version;

/**
 * Un exemplaire physique — la ressource que deux adherents peuvent convoiter en meme temps.
 *
 * <p>{@code @Version} suffit a rendre le verrouillage optimiste operant : Hibernate ajoute
 * {@code and version = ?} a chaque UPDATE. Si la ligne a change entre la lecture et l'ecriture,
 * zero ligne est modifiee, et Hibernate leve une {@code OptimisticLockException}. Aucun verrou
 * n'est pose en base — d'ou le nom : on parie qu'il n'y aura pas de conflit, et on le detecte
 * s'il y en a un.
 */
@Entity
public class Copy {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "copy_seq")
    @SequenceGenerator(name = "copy_seq", sequenceName = "copy_seq", allocationSize = 1)
    private Long id;

    private String barcode;

    private boolean available = true;

    @Version
    private int version;

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

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public boolean isAvailable() {
        return available;
    }

    public int getVersion() {
        return version;
    }

    /**
     * Preter l'exemplaire.
     *
     * @throws IllegalStateException s'il est deja sorti
     */
    public void preter() {
        if (!available) {
            throw new IllegalStateException("exemplaire " + barcode + " deja emprunte");
        }
        available = false;
    }

    public void rendre() {
        available = true;
    }
}
