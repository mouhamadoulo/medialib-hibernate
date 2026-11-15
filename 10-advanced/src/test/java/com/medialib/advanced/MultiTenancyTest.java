package com.medialib.advanced;

import static org.assertj.core.api.Assertions.assertThat;

import com.medialib.support.MediaLibPostgres;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Multi-tenancy par schema : chaque agence du reseau a son propre catalogue, invisible des
 * autres.
 *
 * <p>Ce test batit sa propre {@code SessionFactory} : le multi-tenancy ne se declare pas par
 * un reglage mais par un {@link SchemaParAgenceProvider} enregistre comme service, ce que la
 * classe de base des autres chapitres ne fait pas.
 *
 * <p>Le point a retenir : <strong>aucune entite, aucune requete de ce test ne mentionne
 * l'agence</strong>. L'isolation vient de la connexion.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultiTenancyTest {

    private static final String DAKAR = "agence_dakar";
    private static final String SAINT_LOUIS = "agence_saint_louis";

    private StandardServiceRegistry registry;
    private SessionFactory sessionFactory;

    @BeforeAll
    void demarre() throws Exception {
        PostgreSQLContainer<?> postgres = MediaLibPostgres.instance();

        SchemaParAgenceProvider fournisseur = new SchemaParAgenceProvider(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword(), "public");
        fournisseur.prepareLeSchema(DAKAR);
        fournisseur.prepareLeSchema(SAINT_LOUIS);

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, fournisseur);
        settings.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, new ResolveurDAgence());
        settings.put(AvailableSettings.HBM2DDL_AUTO, "none"); // les schemas sont deja prepares
        settings.put(AvailableSettings.SHOW_SQL, false);

        registry = new StandardServiceRegistryBuilder().applySettings(settings).build();
        sessionFactory = new MetadataSources(registry)
                .addAnnotatedClass(TenantMedia.class)
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
    @DisplayName("Chaque agence ne voit que son propre catalogue")
    void chaque_agence_ne_voit_que_son_schema() {
        inAgence(DAKAR, session -> {
            session.persist(new TenantMedia(1L, "Une si longue lettre"));
            session.persist(new TenantMedia(2L, "L'Aventure ambigue"));
        });

        inAgence(SAINT_LOUIS, session ->
                session.persist(new TenantMedia(1L, "Les Bouts de bois de Dieu")));

        // Meme requete, meme identifiant, deux resultats differents.
        List<String> dakar = titres(DAKAR);
        List<String> saintLouis = titres(SAINT_LOUIS);

        assertThat(dakar).containsExactlyInAnyOrder(
                "Une si longue lettre", "L'Aventure ambigue");
        assertThat(saintLouis).containsExactly("Les Bouts de bois de Dieu");
    }

    @Test
    @DisplayName("Le meme identifiant designe deux oeuvres differentes selon l'agence")
    void le_meme_identifiant_coexiste_dans_deux_agences() {
        inAgence(DAKAR, session -> session.persist(new TenantMedia(10L, "Oeuvre de Dakar")));
        inAgence(SAINT_LOUIS, session ->
                session.persist(new TenantMedia(10L, "Oeuvre de Saint-Louis")));

        String aDakar = inAgenceReturning(DAKAR, session ->
                session.find(TenantMedia.class, 10L).getTitle());
        String aSaintLouis = inAgenceReturning(SAINT_LOUIS, session ->
                session.find(TenantMedia.class, 10L).getTitle());

        assertThat(aDakar).isEqualTo("Oeuvre de Dakar");
        assertThat(aSaintLouis).isEqualTo("Oeuvre de Saint-Louis");
    }

    @Test
    @DisplayName("La session connait son tenant, et il n'est pas modifiable")
    void la_session_porte_son_identifiant_de_tenant() {
        try (Session session = sessionFactory.withOptions().tenantIdentifier(DAKAR).openSession()) {
            assertThat(session.getTenantIdentifier()).isEqualTo(DAKAR);
        }
    }

    @Test
    @DisplayName("Supprimer dans une agence ne touche pas l'autre")
    void une_suppression_reste_confinee_a_son_agence() {
        inAgence(DAKAR, session -> session.persist(new TenantMedia(20L, "A supprimer")));
        inAgence(SAINT_LOUIS, session -> session.persist(new TenantMedia(20L, "A conserver")));

        inAgence(DAKAR, session -> session.remove(session.find(TenantMedia.class, 20L)));

        TenantMedia supprimee =
                inAgenceReturning(DAKAR, session -> session.find(TenantMedia.class, 20L));
        String conservee = inAgenceReturning(SAINT_LOUIS,
                session -> session.find(TenantMedia.class, 20L).getTitle());

        assertThat(supprimee).isNull();
        assertThat(conservee).isEqualTo("A conserver");
    }

    private List<String> titres(String agence) {
        return inAgenceReturning(agence, session -> session
                .createQuery("select m.title from TenantMedia m", String.class).getResultList());
    }

    private void inAgence(String agence, java.util.function.Consumer<Session> travail) {
        try (Session session = sessionFactory.withOptions().tenantIdentifier(agence).openSession()) {
            session.beginTransaction();
            travail.accept(session);
            session.getTransaction().commit();
        }
    }

    private <T> T inAgenceReturning(String agence, java.util.function.Function<Session, T> travail) {
        try (Session session = sessionFactory.withOptions().tenantIdentifier(agence).openSession()) {
            session.beginTransaction();
            T resultat = travail.apply(session);
            session.getTransaction().commit();
            return resultat;
        }
    }

    /**
     * Resout le tenant « courant » quand personne ne le precise.
     *
     * <p>Dans une application web, il viendrait de l'en-tete HTTP, du sous-domaine ou du jeton
     * d'authentification. Ici les tests le passent explicitement, donc ce resolveur ne sert que
     * de garde-fou.
     */
    private static class ResolveurDAgence implements CurrentTenantIdentifierResolver<String> {

        @Override
        public String resolveCurrentTenantIdentifier() {
            return DAKAR;
        }

        @Override
        public boolean validateExistingCurrentSessions() {
            return false;
        }
    }
}
