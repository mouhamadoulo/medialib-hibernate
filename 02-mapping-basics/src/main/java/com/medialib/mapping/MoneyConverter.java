package com.medialib.mapping;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persiste un {@link Money} en centimes, dans une colonne entiere.
 *
 * <p>Stocker un montant en virgule flottante est une faute classique : 0.1 + 0.2 ne fait pas
 * 0.3. Un entier de centimes est exact.
 *
 * <p>{@code autoApply = true} : tout attribut de type {@code Money} est converti sans avoir a
 * annoter chaque champ.
 */
@Converter(autoApply = true)
public class MoneyConverter implements AttributeConverter<Money, Long> {

    @Override
    public Long convertToDatabaseColumn(Money attribute) {
        return attribute == null ? null : attribute.toCents();
    }

    @Override
    public Money convertToEntityAttribute(Long dbData) {
        return dbData == null ? null : Money.fromCents(dbData);
    }
}
