package com.medialib.advanced;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Une oeuvre, dans un catalogue propre a chaque agence.
 *
 * <p>L'entite ne porte <strong>aucune</strong> trace du tenant : pas de colonne
 * {@code agence}, pas de filtre. C'est tout l'interet du multi-tenancy par schema — le code
 * metier ignore qu'il existe plusieurs agences, et aucune requete ne peut « oublier » de
 * filtrer. L'isolation est garantie par la connexion, pas par la discipline du developpeur.
 *
 * <p>L'identifiant est fourni par l'appelant : chaque agence numerote son propre catalogue,
 * et une sequence partagee entre schemas n'aurait pas de sens ici.
 */
@Entity
@Table(name = "tenant_media")
public class TenantMedia {

    @Id
    private Long id;

    private String title;

    protected TenantMedia() {
    }

    public TenantMedia(Long id, String title) {
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
