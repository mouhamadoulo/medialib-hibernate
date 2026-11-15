package com.medialib.advanced;

import com.medialib.support.HibernateTestBase;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;

/** Les entites de ce chapitre, et l'enregistrement du listener d'horodatage. */
abstract class AvanceTestBase extends HibernateTestBase {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Media.class, Copy.class, Loan.class, Member.class, RevisionAvecAuteur.class
        };
    }

    @Override
    protected Map<String, Object> extraSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                CamelCaseToUnderscoresNamingStrategy.class.getName());
        // Envers nomme les tables d'audit <table>_aud par defaut ; on le rend explicite.
        settings.put("org.hibernate.envers.audit_table_suffix", "_aud");
        return settings;
    }

    /**
     * Un listener d'evenements ne se declare pas par un reglage : il s'enregistre sur la
     * fabrique, apres sa construction.
     */
    @Override
    protected void apresDemarrage(SessionFactory sessionFactory) {
        EventListenerRegistry registre = sessionFactory
                .unwrap(SessionFactoryImplementor.class)
                .getEventEngine()
                .getListenerRegistry();
        registre.appendListeners(EventType.PRE_INSERT, new HorodatageListener());
    }
}
