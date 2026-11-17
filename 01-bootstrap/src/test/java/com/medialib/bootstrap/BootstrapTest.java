package com.medialib.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.medialib.support.MediaLibPostgres;
import com.medialib.support.SqlStatementCounter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Les trois facons de demarrer Hibernate, et ce qui les distingue.
 *
 * <p>API programmatique (registre + {@code MetadataSources}), API standard JPA
 * ({@code persistence.xml} + {@code EntityManagerFactory}), et {@code hibernate.cfg.xml} lu
 * par {@link org.hibernate.cfg.Configuration}. La troisieme est la plus ancienne et reste la
 * seule ou la liste des entites est declaree hors du code.
 *
 * <p>Cette classe n'etend pas {@code HibernateTestBase} : son objet est precisement de batir
 * les fabriques a la main, ce que la classe de base fait pour tous les autres chapitres.
 */
class BootstrapTest {

    private Map<String, Object> reglagesDeConnexion() {
        PostgreSQLContainer<?> postgres = MediaLibPostgres.instance();
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put(AvailableSettings.JAKARTA_JDBC_URL, postgres.getJdbcUrl());
        settings.put(AvailableSettings.JAKARTA_JDBC_USER, postgres.getUsername());
        settings.put(AvailableSettings.JAKARTA_JDBC_PASSWORD, postgres.getPassword());
        settings.put(AvailableSettings.JAKARTA_JDBC_DRIVER, postgres.getDriverClassName());
        settings.put(AvailableSettings.HBM2DDL_AUTO, "create-drop");
        settings.put(AvailableSettings.SHOW_SQL, false);
        return settings;
    }

    @Test
    @DisplayName("Bootstrap natif : StandardServiceRegistryBuilder puis MetadataSources")
    void demarre_une_session_factory_par_api_programmatique() {
        SqlStatementCounter counter = new SqlStatementCounter();
        Map<String, Object> settings = reglagesDeConnexion();
        settings.put(AvailableSettings.STATEMENT_INSPECTOR, counter);

        StandardServiceRegistry registry =
                new StandardServiceRegistryBuilder().applySettings(settings).build();
        try (SessionFactory sessionFactory = new MetadataSources(registry)
                .addAnnotatedClass(Member.class)
                .addAnnotatedClass(Branch.class)
                .buildMetadata()
                .buildSessionFactory()) {

            assertThat(sessionFactory.isOpen()).isTrue();

            counter.reset();
            Long id = sessionFactory.fromTransaction(session -> {
                Branch branch = new Branch("Mediatheque centrale", "Dakar");
                session.persist(branch);
                return branch.getId();
            });

            assertThat(id).isNotNull();
            assertThat(counter.insertCount()).isEqualTo(1);
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @Test
    @DisplayName("Bootstrap JPA : persistence.xml et EntityManagerFactory")
    void demarre_une_session_factory_par_persistence_xml() {
        EntityManagerFactory factory =
                Persistence.createEntityManagerFactory("medialib-bootstrap", reglagesDeConnexion());
        try {
            assertThat(factory.isOpen()).isTrue();

            try (EntityManager em = factory.createEntityManager()) {
                em.getTransaction().begin();
                Branch branch = new Branch("Annexe Nord", "Saint-Louis");
                em.persist(branch);
                em.getTransaction().commit();

                assertThat(branch.getId()).isNotNull();
            }
        } finally {
            factory.close();
        }
    }

    @Test
    @DisplayName("Bootstrap par hibernate.cfg.xml : la liste des entites vit dans le XML")
    void demarre_une_session_factory_par_hibernate_cfg_xml() {
        SqlStatementCounter counter = new SqlStatementCounter();

        // `configure()` sans argument lit `hibernate.cfg.xml` a la racine du classpath. Les
        // reglages de connexion, eux, ne peuvent pas y etre ecrits : l'URL du conteneur
        // Testcontainers n'est connue qu'a l'execution. On les ajoute donc apres coup — et
        // c'est tout l'interet de cette forme : le XML porte ce qui est stable (les entites),
        // le code porte ce qui ne l'est pas.
        Configuration configuration = new Configuration().configure();
        configuration.getProperties().putAll(reglagesDeConnexion());
        configuration.getProperties().put(AvailableSettings.STATEMENT_INSPECTOR, counter);

        try (SessionFactory sessionFactory = configuration.buildSessionFactory()) {
            // Aucun addAnnotatedClass n'a ete appele ici : les deux entites viennent des
            // balises <mapping class="..."/> du fichier.
            assertThat(sessionFactory.getMetamodel().getEntities())
                    .extracting(type -> type.getJavaType().getSimpleName())
                    .containsExactlyInAnyOrder("Member", "Branch");

            counter.reset();
            sessionFactory.inTransaction(session ->
                    session.persist(new Branch("Annexe Sud", "Ziguinchor")));

            assertThat(counter.insertCount()).isEqualTo(1);
            assertThat(counter.lastQuery()).contains("insert into branch");
        }
    }

    @Test
    @DisplayName("Session etend EntityManager : le natif est un surensemble du standard")
    void session_est_un_entity_manager() {
        assertThat(EntityManager.class).isAssignableFrom(Session.class);

        EntityManagerFactory factory =
                Persistence.createEntityManagerFactory("medialib-bootstrap", reglagesDeConnexion());
        try (EntityManager em = factory.createEntityManager()) {
            // Un EntityManager obtenu par l'API standard EST une Session Hibernate.
            Session session = em.unwrap(Session.class);

            assertThat(session).isSameAs(em);
        } finally {
            factory.close();
        }
    }
}
