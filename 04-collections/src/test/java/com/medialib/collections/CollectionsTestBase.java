package com.medialib.collections;

import com.medialib.support.HibernateTestBase;
import java.util.Map;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;

/** Les entites de ce chapitre. */
abstract class CollectionsTestBase extends HibernateTestBase {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {Media.class, Copy.class, Review.class};
    }

    @Override
    protected Map<String, Object> extraSettings() {
        return Map.of(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                CamelCaseToUnderscoresNamingStrategy.class.getName());
    }
}
