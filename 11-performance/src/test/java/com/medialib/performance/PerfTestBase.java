package com.medialib.performance;

import com.medialib.support.MediaLibPostgres;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Socle propre a ce chapitre : HikariCP, enveloppe par {@code datasource-proxy}.
 *
 * <p>Les dix autres chapitres laissent Hibernate gerer ses connexions, pour que leur bootstrap
 * reste canonique. Ici, le pool et le proxy <strong>sont</strong> le sujet : on veut compter
 * les allers-retours JDBC, et dimensionner un pool pour de vrai.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class PerfTestBase {

    protected static final int TAILLE_DE_LOT = 20;

    private final CompteurDExecutions compteur = new CompteurDExecutions();

    private HikariDataSource pool;
    private StandardServiceRegistry registry;
    private SessionFactory sessionFactory;

    /** Reglages Hibernate du chapitre. Les sous-classes les completent. */
    protected Map<String, Object> reglages() {
        return Map.of();
    }

    protected Class<?>[] entities() {
        return new Class<?>[] {Media.class, Tag.class};
    }

    protected SessionFactory sessionFactory() {
        demarreSiNecessaire();
        return sessionFactory;
    }

    protected CompteurDExecutions jdbc() {
        return compteur;
    }

    protected void inTransaction(Consumer<Session> travail) {
        sessionFactory().inTransaction(travail::accept);
    }

    protected <T> T inTransactionReturning(Function<Session, T> travail) {
        return sessionFactory().fromTransaction(travail::apply);
    }

    @BeforeEach
    void repartDUneBasePropre() {
        demarreSiNecessaire();
        sessionFactory.inTransaction(session -> session
                .createNativeMutationQuery("truncate table media, tag restart identity cascade")
                .executeUpdate());
        compteur.reset();
    }

    @AfterAll
    void arrete() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
            registry = null;
        }
        if (pool != null) {
            pool.close();
            pool = null;
        }
    }

    private void demarreSiNecessaire() {
        if (sessionFactory != null) {
            return;
        }
        PostgreSQLContainer<?> postgres = MediaLibPostgres.instance();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        // Dimensionnement : le pool n'a pas a etre grand, il a a etre juste. Un pool plus
        // large que le nombre de requetes paralleles ne fait qu'ajouter de la contention
        // cote base. Pour des tests sequentiels, quatre connexions suffisent largement.
        config.setMaximumPoolSize(4);
        config.setPoolName("medialib-perf");
        pool = new HikariDataSource(config);

        DataSource avecProxy = ProxyDataSourceBuilder.create(pool)
                .name("medialib")
                .listener(compteur)
                .build();

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put(AvailableSettings.DATASOURCE, avecProxy);
        settings.put(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                CamelCaseToUnderscoresNamingStrategy.class.getName());
        settings.put(AvailableSettings.HBM2DDL_AUTO, "create-drop");
        settings.put(AvailableSettings.SHOW_SQL, false);
        settings.putAll(reglages());

        registry = new StandardServiceRegistryBuilder().applySettings(settings).build();

        MetadataSources sources = new MetadataSources(registry);
        for (Class<?> entite : entities()) {
            sources.addAnnotatedClass(entite);
        }
        sessionFactory = sources.buildMetadata().buildSessionFactory();
    }

    /** Journalise une duree. Jamais assertee — voir le README. */
    protected static void journalise(String quoi, long debutNanos, int lignes) {
        long millis = (System.nanoTime() - debutNanos) / 1_000_000;
        System.out.printf("[perf] %-42s %6d lignes en %5d ms%n", quoi, lignes, millis);
    }
}
