package com.medialib.mapping;

import jakarta.persistence.Embeddable;

/**
 * L'adresse d'un adherent.
 *
 * <p>Type valeur : elle n'a pas d'existence propre, pas d'identifiant, et disparait avec son
 * adherent. Un {@code @Embeddable} est aplati dans la table de l'entite proprietaire — aucune
 * table, aucune jointure.
 */
@Embeddable
public class Address {

    private String street;

    private String city;

    private String postalCode;

    protected Address() {
        // requis par Hibernate
    }

    public Address(String street, String city, String postalCode) {
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }
}
