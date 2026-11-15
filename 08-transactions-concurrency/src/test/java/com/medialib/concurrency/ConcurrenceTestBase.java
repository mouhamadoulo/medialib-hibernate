package com.medialib.concurrency;

import com.medialib.support.HibernateTestBase;
import java.util.Map;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;

/** Les entites de ce chapitre. */
abstract class ConcurrenceTestBase extends HibernateTestBase {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {Copy.class, Member.class, Loan.class};
    }

    @Override
    protected Map<String, Object> extraSettings() {
        return Map.of(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                CamelCaseToUnderscoresNamingStrategy.class.getName());
    }

    /** Un exemplaire disponible, et son identifiant. */
    protected Long unExemplaireDisponible(String codeBarres) {
        return inTransactionReturning(session -> {
            Copy copy = new Copy(codeBarres);
            session.persist(copy);
            return copy.getId();
        });
    }

    protected Long unAdherent(String nom) {
        return inTransactionReturning(session -> {
            Member member = new Member(nom);
            session.persist(member);
            return member.getId();
        });
    }
}
