package com.medialib.caching;

import com.medialib.support.HibernateTestBase;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

/**
 * Le cache de second niveau, branche via JCache et Caffeine.
 *
 * <p>Hibernate ne cache rien lui-meme : il delegue a un fournisseur JCache. Caffeine est le
 * choix par defaut raisonnable pour un cache local — rapide, borne, sans dependance reseau.
 * Un cache distribue (Infinispan, Hazelcast) se brancherait exactement ici, sans toucher aux
 * entites.
 */
abstract class CacheTestBase extends HibernateTestBase {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {Media.class, Author.class, Branch.class, Loan.class};
    }

    @Override
    protected Map<String, Object> extraSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                CamelCaseToUnderscoresNamingStrategy.class.getName());
        settings.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, true);
        settings.put(AvailableSettings.CACHE_REGION_FACTORY, "jcache");
        settings.put("hibernate.javax.cache.provider",
                "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider");
        settings.put(AvailableSettings.USE_QUERY_CACHE, true);
        settings.put(AvailableSettings.GENERATE_STATISTICS, true);
        return settings;
    }

    protected Statistics stats() {
        return sessionFactory().getStatistics();
    }

    /** Remet les compteurs a zero : ils sont cumulatifs sur toute la vie de la fabrique. */
    protected void videLesStatistiques() {
        stats().clear();
    }
}
