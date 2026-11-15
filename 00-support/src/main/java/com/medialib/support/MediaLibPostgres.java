package com.medialib.support;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Le conteneur PostgreSQL du module courant.
 *
 * <p><strong>Un conteneur par module, pas un pour tout le projet.</strong> Surefire forke une
 * JVM par module Maven : un singleton Java ne donnerait donc pas un conteneur partage, mais un
 * par module de toute facon. Et c'est tant mieux — chaque chapitre remappe les memes tables
 * differemment ({@code media} est mappe d'une facon au module 02, de trois facons au module 05),
 * donc une base par module est la seule facon d'eviter les collisions de schema.
 *
 * <p>Le mode <em>reuse</em> de Testcontainers accelere les allers-retours locaux. Il est
 * facultatif : il faut l'activer soi-meme dans {@code ~/.testcontainers.properties} avec
 * {@code testcontainers.reuse.enable=true}. Sans ca, ce reglage est ignore et le conteneur est
 * arrete en fin de JVM.
 */
public final class MediaLibPostgres {

    private static final DockerImageName IMAGE = DockerImageName.parse("postgres:17-alpine");

    private static PostgreSQLContainer<?> container;

    private MediaLibPostgres() {
    }

    /** Demarre le conteneur au premier appel, puis le rend tel quel. */
    public static synchronized PostgreSQLContainer<?> instance() {
        if (container == null) {
            container = demarre();
        }
        return container;
    }

    private static PostgreSQLContainer<?> demarre() {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(IMAGE)
                .withDatabaseName("medialib")
                .withUsername("medialib")
                .withPassword("medialib")
                .withReuse(true);
        try {
            postgres.start();
            return postgres;
        } catch (RuntimeException echec) {
            throw new IllegalStateException(
                    """
                    Impossible de demarrer le conteneur PostgreSQL.

                    Ce projet execute ses tests contre un vrai PostgreSQL : H2 simulerait les
                    verrous pessimistes, les sequences et les schemas multi-tenants, ce qui
                    fausserait les chapitres 08, 10 et 11.

                    Verifie que Docker Desktop est demarre :  docker info
                    """,
                    echec);
        }
    }
}
