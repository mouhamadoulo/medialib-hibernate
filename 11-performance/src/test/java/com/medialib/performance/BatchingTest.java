package com.medialib.performance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le batching JDBC : regrouper N instructions en un seul aller-retour.
 *
 * <p>C'est le levier de performance le plus rentable d'Hibernate, et l'un des plus souvent
 * inoperant — parce qu'une seule annotation mal choisie le desactive sans rien dire.
 */
class BatchingTest extends PerfTestBase {

    @Override
    protected Map<String, Object> reglages() {
        return Map.of(
                AvailableSettings.STATEMENT_BATCH_SIZE, TAILLE_DE_LOT,
                AvailableSettings.ORDER_INSERTS, true,
                AvailableSettings.ORDER_UPDATES, true);
    }

    @Test
    @DisplayName("Avec batch_size=20 : 100 insertions en 5 allers-retours")
    void batch_size_regroupe_les_insertions() {
        inTransaction(session -> {
            jdbc().reset();

            for (int i = 1; i <= 100; i++) {
                session.persist(new Media("Oeuvre " + i));
            }
            session.flush();

            // Cent INSERT logiques...
            assertThat(jdbc().requetesDe("insert")).isEqualTo(100);
            // ...mais seulement cinq allers-retours reseau pour les porter. C'est tout le gain.
            assertThat(jdbc().executionsDe("insert")).isEqualTo(100 / TAILLE_DE_LOT);

            // Et trois SELECT seulement pour les cent identifiants : c'est
            // allocationSize = 50 qui les economise. Sans lui, on aurait cent appels a la
            // sequence, et le batching des INSERT n'y changerait rien — les allers-retours
            // viendraient des identifiants.
            assertThat(jdbc().executionsDe("select")).isLessThanOrEqualTo(3);
        });
    }

    @Test
    @DisplayName("IDENTITY desactive le batching en silence : 100 insertions, 100 allers-retours")
    void identity_desactive_silencieusement_le_batching() {
        inTransaction(session -> {
            jdbc().reset();

            for (int i = 1; i <= 100; i++) {
                session.persist(new Tag("mot-cle-" + i));
            }
            session.flush();

            // Meme reglage batch_size=20, meme transaction, meme volume. Et pourtant :
            assertThat(jdbc().executionsDe("insert")).isEqualTo(100);

            // La cause : la base attribue la cle au moment de l'INSERT. Hibernate doit donc
            // emettre chaque INSERT separement pour relire la valeur generee. Aucune
            // configuration ne peut contourner ca — il faut changer de generateur.
        });
    }

    @Test
    @DisplayName("Le batching des UPDATE fonctionne aussi, et il est plus souvent oublie")
    void batch_size_regroupe_aussi_les_updates() {
        inTransaction(session -> {
            for (int i = 1; i <= 60; i++) {
                session.persist(new Media("Oeuvre " + i));
            }
        });

        inTransaction(session -> {
            var oeuvres = session.createQuery("select m from Media m", Media.class).getResultList();
            jdbc().reset();

            oeuvres.forEach(m -> m.setLoanCount(m.getLoanCount() + 1));
            session.flush();

            assertThat(jdbc().requetesDe("update")).isEqualTo(60);
            assertThat(jdbc().executionsDe("update")).isEqualTo(60 / TAILLE_DE_LOT);
        });
    }
}
