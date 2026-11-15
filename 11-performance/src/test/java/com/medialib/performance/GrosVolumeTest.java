package com.medialib.performance;

import static org.assertj.core.api.Assertions.assertThat;

import com.medialib.support.Slow;
import java.util.Map;
import org.hibernate.StatelessSession;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Cent mille insertions, pour de vrai — et la mauvaise surprise du chapitre.
 *
 * <p>Taggue {@code slow} : exclu du build par defaut, active par {@code mvn verify -Pslow}.
 *
 * <p>Les durees sont <strong>journalisees et jamais assertees</strong>. Seul le nombre
 * d'allers-retours JDBC est asserte, parce que c'est une grandeur deterministe : elle sera la
 * meme sur ta machine et sur la mienne, alors que les millisecondes ne le seront jamais.
 */
@Tag(Slow.SLOW)
class GrosVolumeTest extends PerfTestBase {

    private static final int LIGNES = 100_000;
    private static final int LOT = 500;

    /** Volume reduit pour la comparaison : sans batching, chaque ligne coute un aller-retour. */
    private static final int LIGNES_SANS_BATCH = 10_000;

    @Override
    protected Map<String, Object> reglages() {
        return Map.of(
                AvailableSettings.STATEMENT_BATCH_SIZE, LOT,
                AvailableSettings.ORDER_INSERTS, true);
    }

    @Test
    @DisplayName("100 000 insertions en Session + batch + flush/clear : 200 allers-retours")
    void cent_mille_insertions_en_batch() {
        long debut = System.nanoTime();

        inTransaction(session -> {
            jdbc().reset();
            for (int i = 1; i <= LIGNES; i++) {
                session.persist(new Media("Oeuvre " + i));
                if (i % LOT == 0) {
                    // Les deux lignes qui rendent l'operation tenable : sans elles, le
                    // contexte de persistance garde cent mille entites, et le dirty checking
                    // les compare toutes a chaque flush.
                    session.flush();
                    session.clear();
                }
            }
        });
        journalise("100k insertions, Session + batch + flush/clear", debut, LIGNES);

        // Le contrat deterministe : 100 000 INSERT, regroupes par 500.
        assertThat(jdbc().requetesDe("insert")).isEqualTo(LIGNES);
        assertThat(jdbc().executionsDe("insert")).isEqualTo(LIGNES / LOT);

        Long enBase = inTransactionReturning(session ->
                session.createQuery("select count(m) from Media m", Long.class).getSingleResult());
        assertThat(enBase).isEqualTo(LIGNES);
    }

    @Test
    @DisplayName("La mauvaise surprise : StatelessSession n'emet AUCUN lot")
    void une_stateless_session_n_emet_aucun_lot() {
        long debut = System.nanoTime();

        try (StatelessSession stateless = sessionFactory().openStatelessSession()) {
            var transaction = stateless.beginTransaction();
            jdbc().reset();

            for (int i = 1; i <= LIGNES_SANS_BATCH; i++) {
                stateless.insert(new Media("Oeuvre " + i));
            }

            transaction.commit();
        }
        journalise("10k insertions, StatelessSession.insert() un par un", debut,
                LIGNES_SANS_BATCH);

        // Un aller-retour par ligne, malgre batch_size = 500.
        //
        // La cause est structurelle : une StatelessSession n'a pas de file d'actions
        // differees. C'est meme sa definition — pas de contexte de persistance, donc rien a
        // accumuler, donc rien a regrouper. Chaque insert() part immediatement.
        //
        // Conclusion a retenir : StatelessSession sert a LIRE de gros volumes sans remplir
        // la memoire, pas a en ECRIRE vite. Pour l'ecriture en masse, la bonne combinaison
        // est celle du test precedent : Session ordinaire + batch_size + flush/clear
        // periodiques.
        //
        // Ordre de grandeur releve sur la machine de developpement (jamais asserte) :
        // environ 19 000 lignes/s en Session + batch, contre 1 100 lignes/s en
        // StatelessSession ligne par ligne. Plus d'un ordre de grandeur.
        assertThat(jdbc().executionsDe("insert")).isEqualTo(LIGNES_SANS_BATCH);
        assertThat(jdbc().requetesDe("insert")).isEqualTo(LIGNES_SANS_BATCH);

        Long enBase = inTransactionReturning(session ->
                session.createQuery("select count(m) from Media m", Long.class).getSingleResult());
        assertThat(enBase).isEqualTo(LIGNES_SANS_BATCH);
    }

    @Test
    @DisplayName("Ce a quoi StatelessSession sert vraiment : lire sans remplir la memoire")
    void une_stateless_session_lit_sans_remplir_le_contexte() {
        inTransaction(session -> {
            for (int i = 1; i <= LIGNES_SANS_BATCH; i++) {
                session.persist(new Media("Oeuvre " + i));
                if (i % LOT == 0) {
                    session.flush();
                    session.clear();
                }
            }
        });

        long debut = System.nanoTime();
        int lues = 0;
        try (StatelessSession stateless = sessionFactory().openStatelessSession()) {
            var transaction = stateless.beginTransaction();
            jdbc().reset();

            try (var flux = stateless.createQuery("select m from Media m", Media.class)
                    .setFetchSize(LOT)
                    .getResultStream()) {
                lues = (int) flux.filter(m -> m.getTitle() != null).count();
            }
            transaction.commit();
        }
        journalise("10k lectures, StatelessSession en streaming", debut, lues);

        assertThat(lues).isEqualTo(LIGNES_SANS_BATCH);
        // Une seule requete, et aucune entite retenue : c'est la que StatelessSession gagne.
        assertThat(jdbc().executionsDe("select")).isEqualTo(1);
    }
}
