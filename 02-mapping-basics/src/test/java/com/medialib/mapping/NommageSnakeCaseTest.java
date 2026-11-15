package com.medialib.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import com.medialib.support.HibernateTestBase;
import java.util.Map;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La meme entite que {@code NommageTest}, avec la strategie de nommage physique installee.
 *
 * <p>C'est celle que Spring Boot active par defaut, et celle qu'utilise le reste de ce
 * chapitre. Une seule ligne de configuration separe les deux resultats.
 */
class NommageSnakeCaseTest extends HibernateTestBase {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {CatalogEntry.class};
    }

    @Override
    protected Map<String, Object> extraSettings() {
        return Map.of(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                CamelCaseToUnderscoresNamingStrategy.class.getName());
    }

    @Test
    @DisplayName("Avec la strategie, shelfLocation devient shelf_location")
    void la_strategie_camel_vers_underscores_produit_du_snake_case() {
        inTransaction(session -> session.persist(new CatalogEntry("R-12-A", 3)));

        inTransaction(session -> {
            sql().reset();
            session.createQuery("select c from CatalogEntry c", CatalogEntry.class).getResultList();

            assertThat(sql().lastQuery()).contains("shelf_location", "copy_count");
        });
    }

    @Test
    @DisplayName("Le nom de table aussi : CatalogEntry devient catalog_entry")
    void le_nom_de_table_devient_snake_case() {
        inTransaction(session -> session.persist(new CatalogEntry("R-12-A", 3)));

        Long compte = inTransactionReturning(session ->
                session.createNativeQuery("select count(*) from catalog_entry", Long.class)
                        .getSingleResult());

        assertThat(compte).isEqualTo(1L);
    }
}
