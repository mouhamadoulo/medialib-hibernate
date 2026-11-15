package com.medialib.mapping;

import java.io.Serializable;

/**
 * Un ISBN.
 *
 * <p>Saisi avec ou sans tirets ({@code 978-2-07-036822-8} ou {@code 9782070368228}), il
 * designe le meme ouvrage. La forme normalisee est la seule qui doit atteindre la base, sinon
 * deux saisies du meme livre ne se retrouvent pas.
 *
 * <p>{@link Serializable} parce qu'un {@code UserType} doit pouvoir desassembler sa valeur
 * pour le cache de second niveau.
 */
public record Isbn(String value) implements Serializable {

    public Isbn {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ISBN vide");
        }
        value = value.replace("-", "").replace(" ", "").toUpperCase();
    }
}
