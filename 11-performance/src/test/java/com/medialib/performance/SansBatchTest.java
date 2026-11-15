package com.medialib.performance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le temoin : la meme chose, sans {@code hibernate.jdbc.batch_size}.
 *
 * <p>C'est la configuration par defaut d'Hibernate. Autrement dit : le batching n'est pas
 * actif tant qu'on ne l'a pas demande, et beaucoup d'applications l'ignorent toute leur vie.
 */
class SansBatchTest extends PerfTestBase {

    // Aucun reglage de batch : on garde le defaut d'Hibernate.

    @Test
    @DisplayName("Sans batch_size : 100 insertions coutent 100 allers-retours")
    void sans_batch_size_chaque_insertion_est_un_aller_retour() {
        inTransaction(session -> {
            jdbc().reset();

            for (int i = 1; i <= 100; i++) {
                session.persist(new Media("Oeuvre " + i));
            }
            session.flush();

            assertThat(jdbc().requetesDe("insert")).isEqualTo(100);
            assertThat(jdbc().executionsDe("insert")).isEqualTo(100); // un aller-retour chacun
        });
    }
}
