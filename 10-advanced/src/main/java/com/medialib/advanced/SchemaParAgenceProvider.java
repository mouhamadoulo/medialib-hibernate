package com.medialib.advanced;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

/**
 * Un schema PostgreSQL par agence.
 *
 * <p>C'est la piece centrale du multi-tenancy : Hibernate demande une connexion « pour le
 * tenant X », et ce fournisseur la rend positionnee sur le bon schema. Tout le reste —
 * entites, requetes, code metier — ignore qu'il y a plusieurs agences.
 *
 * <p>Les trois modes de multi-tenancy et leur arbitrage :
 *
 * <ul>
 *   <li><strong>Base par tenant</strong> : isolation maximale, cout d'exploitation maximal.</li>
 *   <li><strong>Schema par tenant</strong> (ici) : une seule base, une seule connexion
 *       physique, isolation garantie par la base elle-meme. Le bon compromis dans la plupart
 *       des cas.</li>
 *   <li><strong>Discriminant</strong> (une colonne {@code tenant_id}) : le moins couteux, et
 *       le plus dangereux — une requete qui oublie le filtre expose les donnees d'un autre
 *       client. Hibernate ne le gere pas par ce mecanisme mais par {@code @Filter}.</li>
 * </ul>
 */
public class SchemaParAgenceProvider implements MultiTenantConnectionProvider<String> {

    private final String jdbcUrl;
    private final String user;
    private final String password;
    private final String schemaParDefaut;

    public SchemaParAgenceProvider(String jdbcUrl, String user, String password,
            String schemaParDefaut) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
        this.schemaParDefaut = schemaParDefaut;
    }

    /**
     * Une connexion neuve a chaque appel.
     *
     * <p>Volontairement sans pool : ce chapitre montre le mecanisme du multi-tenancy, pas sa
     * mise en production. Une vraie application placerait ici un pool par tenant, ou un pool
     * unique dont les connexions sont repositionnees a chaque emprunt.
     */
    private Connection nouvelleConnexion() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, user, password);
    }

    /** Une connexion sans tenant : Hibernate s'en sert au demarrage, pour lire les metadonnees. */
    @Override
    public Connection getAnyConnection() throws SQLException {
        return positionne(nouvelleConnexion(), schemaParDefaut);
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        return positionne(nouvelleConnexion(), tenantIdentifier);
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection)
            throws SQLException {
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return MultiTenantConnectionProvider.class.isAssignableFrom(unwrapType)
                || SchemaParAgenceProvider.class.isAssignableFrom(unwrapType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> unwrapType) {
        return isUnwrappableAs(unwrapType) ? (T) this : null;
    }

    /**
     * Le geste decisif, et il tient en une instruction : {@code set search_path}. Toute
     * requete emise sur cette connexion ne verra que les tables de ce schema.
     */
    private static Connection positionne(Connection connection, String schema)
            throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("set search_path to \"" + schema + "\"");
        }
        return connection;
    }

    /** Cree le schema d'une agence et y installe la table du catalogue. */
    public void prepareLeSchema(String schema) throws SQLException {
        try (Connection connection = nouvelleConnexion();
                Statement statement = connection.createStatement()) {
            statement.execute("create schema if not exists \"" + schema + "\"");
            statement.execute("set search_path to \"" + schema + "\"");
            statement.execute("drop table if exists tenant_media");
            statement.execute(
                    "create table tenant_media (id bigint not null primary key, title varchar(255))");
        }
    }

    public Properties proprietesInformatives() {
        Properties proprietes = new Properties();
        proprietes.setProperty("schemaParDefaut", schemaParDefaut);
        return proprietes;
    }
}
