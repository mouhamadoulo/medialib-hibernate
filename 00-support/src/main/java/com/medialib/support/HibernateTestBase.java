package com.medialib.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Classe de base des tests de chapitre.
 *
 * <p>Elle fait trois choses, et rien de plus : batir une {@link SessionFactory} contre le
 * conteneur PostgreSQL du module, y brancher le {@link SqlStatementCounter}, et remettre la
 * base a zero entre deux tests.
 *
 * <p>Elle n'enveloppe deliberement <strong>pas</strong> Hibernate derriere une couche
 * « repository » : les tests appellent {@link Session} directement, parce que c'est l'objet
 * qu'on vient apprendre.
 *
 * <p>Chaque sous-classe declare ses entites via {@link #entities()} et, si le chapitre l'exige
 * (cache de second niveau, multi-tenancy, taille de batch), ses reglages via
 * {@link #extraSettings()}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class HibernateTestBase {

    private final SqlStatementCounter counter = new SqlStatementCounter();

    private SessionFactory sessionFactory;
    private StandardServiceRegistry registry;
    private List<String> tables;

    /** Les entites mappees par ce chapitre. */
    protected abstract Class<?>[] entities();

    /** Reglages Hibernate propres au chapitre. Vide par defaut. */
    protected Map<String, Object> extraSettings() {
        return Map.of();
    }

    protected SessionFactory sessionFactory() {
        demarreSiNecessaire();
        return sessionFactory;
    }

    /** Le compteur SQL de ce test. Remis a zero avant chaque methode. */
    protected SqlStatementCounter sql() {
        return counter;
    }

    protected void inTransaction(Consumer<Session> work) {
        sessionFactory().inTransaction(work::accept);
    }

    protected <T> T inTransactionReturning(Function<Session, T> work) {
        return sessionFactory().fromTransaction(work::apply);
    }

    @BeforeEach
    void repartDUneBasePropre() {
        demarreSiNecessaire();
        videLesTables();
        counter.reset();
    }

    @AfterAll
    void fermeHibernate() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
            registry = null;
        }
    }

    private void demarreSiNecessaire() {
        if (sessionFactory != null) {
            return;
        }
        PostgreSQLContainer<?> postgres = MediaLibPostgres.instance();

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put(AvailableSettings.JAKARTA_JDBC_URL, postgres.getJdbcUrl());
        settings.put(AvailableSettings.JAKARTA_JDBC_USER, postgres.getUsername());
        settings.put(AvailableSettings.JAKARTA_JDBC_PASSWORD, postgres.getPassword());
        settings.put(AvailableSettings.JAKARTA_JDBC_DRIVER, postgres.getDriverClassName());
        settings.put(AvailableSettings.HBM2DDL_AUTO, "create-drop");
        settings.put(AvailableSettings.SHOW_SQL, false);
        settings.put(AvailableSettings.STATEMENT_INSPECTOR, counter);
        settings.putAll(extraSettings());

        registry = new StandardServiceRegistryBuilder().applySettings(settings).build();

        MetadataSources sources = new MetadataSources(registry);
        for (Class<?> entity : entities()) {
            sources.addAnnotatedClass(entity);
        }
        Metadata metadata = sources.buildMetadata();

        tables = releveDesTables(metadata);
        sessionFactory = metadata.buildSessionFactory();
    }

    /**
     * Les tables a vider entre deux tests : celles des entites, plus les tables de jonction et
     * de collection, qu'aucune entite ne declare.
     */
    private static List<String> releveDesTables(Metadata metadata) {
        List<String> noms = new ArrayList<>();
        for (PersistentClass binding : metadata.getEntityBindings()) {
            String nom = binding.getTable().getName();
            if (!noms.contains(nom)) {
                noms.add(nom);
            }
        }
        for (Collection binding : metadata.getCollectionBindings()) {
            String nom = binding.getCollectionTable().getName();
            if (!noms.contains(nom)) {
                noms.add(nom);
            }
        }
        return noms;
    }

    /**
     * Un seul {@code truncate ... restart identity cascade} : il ignore les contraintes de cle
     * etrangere, remet les sequences d'identite a zero, et le compteur SQL l'ecarte comme tout
     * le DDL.
     */
    private void videLesTables() {
        if (tables.isEmpty()) {
            return;
        }
        String liste = String.join(", ", tables.stream().map(t -> "\"" + t + "\"").toList());
        sessionFactory.inTransaction(session ->
                session.createNativeMutationQuery("truncate table " + liste + " restart identity cascade")
                        .executeUpdate());
    }
}
