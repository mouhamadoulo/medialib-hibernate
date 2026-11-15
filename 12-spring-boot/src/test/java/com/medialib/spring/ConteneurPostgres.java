package com.medialib.spring;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Le conteneur PostgreSQL, cable par Spring Boot.
 *
 * <p>C'est l'un des apports concrets de Boot, et le contraste avec les onze autres chapitres
 * est net : la-bas, {@code MediaLibPostgres} demarre le conteneur, puis la classe de base lit
 * son URL, son utilisateur et son mot de passe pour les passer a Hibernate. Ici,
 * {@code @ServiceConnection} suffit — Boot demarre le conteneur, en deduit la {@code DataSource}
 * et l'injecte.
 *
 * <p>Le bean est declare {@code static} pour etre partage par tous les contextes Spring du
 * module : sans ca, chaque jeu de proprietes different (OSIV actif, OSIV desactive) demarrerait
 * son propre conteneur.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ConteneurPostgres {

    private static PostgreSQLContainer<?> partage;

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        if (partage == null) {
            partage = new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("medialib")
                    .withUsername("medialib")
                    .withPassword("medialib")
                    .withReuse(true);
        }
        return partage;
    }
}
