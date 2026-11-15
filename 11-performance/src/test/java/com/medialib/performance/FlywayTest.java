package com.medialib.performance;

import static org.assertj.core.api.Assertions.assertThat;

import com.medialib.support.MediaLibPostgres;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Flyway contre {@code hbm2ddl.auto}.
 *
 * <p>Les dix autres chapitres utilisent {@code hbm2ddl.auto=create-drop} : c'est le bon outil
 * pour un support d'apprentissage, ou le schema est jetable. En production, c'est un autre
 * sujet :
 *
 * <ul>
 *   <li>{@code create-drop} detruit les donnees a chaque demarrage — inutilisable ;</li>
 *   <li>{@code update} est le piege le plus couteux du lot. Il ajoute ce qui manque, mais ne
 *       supprime jamais rien, ne renomme rien, ne corrige aucun type, et n'a aucune idee de
 *       la migration des donnees existantes. Le schema derive silencieusement, serveur par
 *       serveur, jusqu'au jour ou deux environnements ne se ressemblent plus ;</li>
 *   <li>{@code validate} ne touche a rien et verifie que le mapping correspond au schema.
 *       C'est le seul reglage raisonnable quand les migrations sont gerees ailleurs.</li>
 * </ul>
 *
 * <p>Ce test fait donc ce qu'une vraie application fait : Flyway applique les migrations
 * versionnees, Hibernate se contente de valider.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlywayTest {

    private static final String SCHEMA = "catalogue_flyway";

    private String jdbcUrl;
    private String user;
    private String motDePasse;
    private StandardServiceRegistry registry;
    private SessionFactory sessionFactory;

    @BeforeAll
    void migrePuisDemarreHibernate() throws Exception {
        PostgreSQLContainer<?> postgres = MediaLibPostgres.instance();
        jdbcUrl = postgres.getJdbcUrl();
        user = postgres.getUsername();
        motDePasse = postgres.getPassword();

        // On part d'un schema vierge, pour que les migrations s'appliquent reellement.
        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, motDePasse);
                Statement statement = connection.createStatement()) {
            statement.execute("drop schema if exists " + SCHEMA + " cascade");
        }

        Flyway.configure()
                .dataSource(jdbcUrl, user, motDePasse)
                .schemas(SCHEMA)
                .defaultSchema(SCHEMA)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put(AvailableSettings.JAKARTA_JDBC_URL, jdbcUrl);
        settings.put(AvailableSettings.JAKARTA_JDBC_USER, user);
        settings.put(AvailableSettings.JAKARTA_JDBC_PASSWORD, motDePasse);
        settings.put(AvailableSettings.DEFAULT_SCHEMA, SCHEMA);
        settings.put(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                CamelCaseToUnderscoresNamingStrategy.class.getName());
        // Hibernate ne cree rien : il verifie que le schema de Flyway correspond au mapping.
        settings.put(AvailableSettings.HBM2DDL_AUTO, "validate");
        settings.put(AvailableSettings.SHOW_SQL, false);

        registry = new StandardServiceRegistryBuilder().applySettings(settings).build();
        sessionFactory = new MetadataSources(registry)
                .addAnnotatedClass(Media.class)
                .addAnnotatedClass(Tag.class)
                .buildMetadata()
                .buildSessionFactory();
    }

    @AfterAll
    void arrete() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @Test
    @DisplayName("Flyway a applique les deux migrations et garde leur trace")
    void flyway_applique_les_migrations_versionnees() {
        // Attention au filtre : Flyway journalise aussi la CREATION du schema, sous le type
        // SCHEMA et sans numero de version. Compter toutes les lignes donnerait trois.
        Long appliquees = sessionFactory.fromTransaction(session -> session.createNativeQuery(
                "select count(*) from " + SCHEMA + ".flyway_schema_history "
                        + "where success and type = 'SQL'", Long.class).getSingleResult());

        assertThat(appliquees).isEqualTo(2L);

        Long creationDeSchema = sessionFactory.fromTransaction(session -> session.createNativeQuery(
                "select count(*) from " + SCHEMA + ".flyway_schema_history where type = 'SCHEMA'",
                Long.class).getSingleResult());
        assertThat(creationDeSchema).isEqualTo(1L);

        String derniere = sessionFactory.fromTransaction(session -> session.createNativeQuery(
                "select version from " + SCHEMA + ".flyway_schema_history "
                        + "order by installed_rank desc limit 1", String.class).getSingleResult());
        assertThat(derniere).isEqualTo("2");
    }

    @Test
    @DisplayName("hbm2ddl.auto=validate a accepte le schema : mapping et base concordent")
    void le_schema_flyway_correspond_au_mapping_hibernate() {
        // Le fait que @BeforeAll soit alle au bout EST l'assertion : en mode validate,
        // Hibernate refuse de demarrer si une table ou une colonne manque.
        assertThat(sessionFactory.isOpen()).isTrue();

        // Et le schema fonctionne vraiment.
        Long id = sessionFactory.fromTransaction(session -> {
            Media media = new Media("Une si longue lettre");
            session.persist(media);
            return media.getId();
        });
        assertThat(id).isNotNull();

        String titre = sessionFactory.fromTransaction(session ->
                session.find(Media.class, id).getTitle());
        assertThat(titre).isEqualTo("Une si longue lettre");
    }

    @Test
    @DisplayName("L'index de la migration V2 existe : hbm2ddl ne l'aurait jamais cree")
    void la_migration_a_cree_un_index_que_le_mapping_ne_decrit_pas() {
        Long index = sessionFactory.fromTransaction(session -> session.createNativeQuery(
                "select count(*) from pg_indexes where schemaname = :schema "
                        + "and indexname = 'idx_media_title'", Long.class)
                .setParameter("schema", SCHEMA)
                .getSingleResult());

        // Aucune annotation de Media ne mentionne cet index. C'est exactement le genre de
        // decision qui appartient a la base, pas au mapping.
        assertThat(index).isEqualTo(1L);
    }

    @Test
    @DisplayName("Rejouer la migration ne fait rien : Flyway sait ou il s'est arrete")
    void rejouer_flyway_est_sans_effet() {
        var resultat = Flyway.configure()
                .dataSource(jdbcUrl, user, motDePasse)
                .schemas(SCHEMA)
                .defaultSchema(SCHEMA)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(resultat.migrationsExecuted).isZero();
    }
}
