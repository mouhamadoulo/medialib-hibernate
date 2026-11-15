package com.medialib.inheritance.mapped;

import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

/**
 * Champs d'audit partages par plusieurs entites sans lien de parente metier.
 *
 * <p>{@code @MappedSuperclass} n'est <strong>pas</strong> de l'heritage persistant : c'est de
 * la simple reutilisation de mapping. Les colonnes sont recopiees dans chaque table fille, et
 * cette classe n'est ni une entite, ni interrogeable. On ne peut pas ecrire
 * {@code from AbstractAuditable} — il n'y a rien derriere.
 *
 * <p>C'est exactement ce qu'il faut quand les entites n'ont rien a voir entre elles mais
 * partagent des colonnes techniques, et exactement ce qu'il ne faut pas quand on veut les
 * interroger ensemble.
 */
@MappedSuperclass
public abstract class AbstractAuditable {

    private Instant createdAt;

    private String createdBy;

    protected AbstractAuditable() {
    }

    protected AbstractAuditable(Instant createdAt, String createdBy) {
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
