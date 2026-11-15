package com.medialib.associations;

import com.medialib.support.HibernateTestBase;
import java.util.Map;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;

/** Les entites de ce chapitre, declarees une fois pour toutes ses classes de test. */
abstract class AssociationsTestBase extends HibernateTestBase {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Media.class, Copy.class, Author.class, Branch.class,
            Reservation.class, Member.class, MembershipCard.class, Loan.class
        };
    }

    @Override
    protected Map<String, Object> extraSettings() {
        return Map.of(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                CamelCaseToUnderscoresNamingStrategy.class.getName());
    }
}
