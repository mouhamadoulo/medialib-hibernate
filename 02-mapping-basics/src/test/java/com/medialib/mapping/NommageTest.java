package com.medialib.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import com.medialib.support.HibernateTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ce qu'Hibernate deduit <strong>tout seul</strong> des noms Java, sans strategie configuree.
 *
 * <p>Contrairement a une croyance repandue, Hibernate ne produit pas de {@code snake_case}.
 * Il se contente de reprendre le nom de l'attribut, en minuscules : {@code shelfLocation}
 * devient {@code shelflocation}, pas {@code shelf_location}. C'est Spring Boot qui installe
 * {@code CamelCaseToUnderscoresNamingStrategy} par defaut — d'ou la confusion.
 *
 * <p>La consequence pratique : un projet qui migre depuis Spring Boot vers un bootstrap
 * Hibernate nu ne retrouve plus ses colonnes. Voir {@code NommageSnakeCaseTest} pour la
 * meme entite avec la strategie installee.
 */
class NommageTest extends HibernateTestBase {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {CatalogEntry.class};
    }

    @Test
    @DisplayName("Par defaut, Hibernate colle les mots : shelfLocation devient shelflocation")
    void la_strategie_par_defaut_ne_produit_pas_de_snake_case() {
        inTransaction(session -> session.persist(new CatalogEntry("R-12-A", 3)));

        inTransaction(session -> {
            sql().reset();
            session.createQuery("select c from CatalogEntry c", CatalogEntry.class).getResultList();

            assertThat(sql().lastQuery()).contains("shelflocation", "copycount");
            assertThat(sql().lastQuery()).doesNotContain("shelf_location", "copy_count");
        });
    }

    @Test
    @DisplayName("Le nom de table suit la meme regle : la classe, telle quelle")
    void le_nom_de_table_par_defaut_est_le_nom_de_la_classe() {
        inTransaction(session -> session.persist(new CatalogEntry("R-12-A", 3)));

        // La table s'appelle CatalogEntry (PostgreSQL la replie en catalogentry, faute de
        // guillemets), et surement pas catalog_entry.
        Long compte = inTransactionReturning(session ->
                session.createNativeQuery("select count(*) from catalogentry", Long.class)
                        .getSingleResult());

        assertThat(compte).isEqualTo(1L);
    }
}
