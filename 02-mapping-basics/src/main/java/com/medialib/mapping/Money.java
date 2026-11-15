package com.medialib.mapping;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Un montant d'amende.
 *
 * <p>Type valeur immuable, sans identite : deux amendes de 2,50 € sont la meme valeur. C'est
 * exactement ce qu'un {@code AttributeConverter} sait persister — une seule colonne, aucune
 * table.
 *
 * @param amount le montant, en unites principales (euros)
 */
public record Money(BigDecimal amount) {

    public Money {
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money ofEuros(String montant) {
        return new Money(new BigDecimal(montant));
    }

    /** Le montant en centimes : c'est sous cette forme qu'il est stocke. */
    public long toCents() {
        return amount.movePointRight(2).longValueExact();
    }

    public static Money fromCents(long cents) {
        return new Money(BigDecimal.valueOf(cents, 2));
    }
}
