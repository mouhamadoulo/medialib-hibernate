package com.medialib.mapping;

import com.medialib.support.HibernateTestBase;
import java.util.Map;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;

/**
 * Les entites de ce chapitre, declarees une fois pour toutes ses classes de test.
 *
 * <p>La strategie de nommage physique est declaree explicitement. Ce n'est <strong>pas</strong>
 * le defaut d'Hibernate : seul, il colle les mots ({@code postalCode} devient
 * {@code postalcode}). C'est Spring Boot qui, lui, installe cette strategie — raison pour
 * laquelle beaucoup de developpeurs croient que le snake_case vient d'Hibernate.
 * Voir {@code NommageTest}.
 */
abstract class MappingTestBase extends HibernateTestBase {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Media.class, Member.class, Publisher.class, Fine.class, Loan.class, MoneyConverter.class
        };
    }

    @Override
    protected Map<String, Object> extraSettings() {
        return Map.of(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                CamelCaseToUnderscoresNamingStrategy.class.getName());
    }
}
