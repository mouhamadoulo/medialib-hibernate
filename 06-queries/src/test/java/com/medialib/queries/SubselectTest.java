package com.medialib.queries;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Une entite mappee sur une <strong>requete</strong> et non sur une table.
 *
 * <p>{@code @Subselect} donne a Hibernate une vue en lecture seule, sans qu'aucune vue
 * n'existe en base : l'expression est injectee comme sous-requete a chaque interrogation.
 * C'est la bonne reponse quand un agregat est lu partout dans l'application et qu'on ne veut
 * ni le recalculer a la main ni creer un objet de base supplementaire.
 *
 * <p>Son piege est ailleurs, et {@code @Synchronize} est la pour lui : Hibernate ne peut pas
 * deviner de quelles tables cette requete depend, donc il ne sait pas quand il doit vider ses
 * ecritures en attente avant de la jouer.
 */
class SubselectTest extends RequetesTestBase {

    @Test
    @DisplayName("Aucune table n'est creee : l'entite n'existe que comme requete")
    void le_subselect_ne_cree_aucun_objet_en_base() {
        List<String> objets = inTransactionReturning(session -> session.createNativeQuery(
                "select table_name from information_schema.tables "
                        + "where table_schema = current_schema() "
                        + "and table_name = 'media_loan_stats'",
                String.class).getResultList());

        // Ni table, ni vue. Hibernate n'a rien a creer : le mapping EST la requete.
        assertThat(objets).isEmpty();
    }

    @Test
    @DisplayName("La requete du mapping est injectee comme sous-requete a chaque lecture")
    void le_subselect_apparait_dans_le_sql_emis() {
        unCatalogue();
        sql().reset();

        List<MediaLoanStats> stats = inTransactionReturning(session -> session
                .createQuery("from MediaLoanStats s order by s.loanCount desc", MediaLoanStats.class)
                .getResultList());

        // Le catalogue commun du chapitre : trois oeuvres, et « Une si longue lettre »
        // empruntee deux fois.
        assertThat(stats).hasSize(3);
        assertThat(stats.get(0).getTitle()).isEqualTo("Une si longue lettre");
        assertThat(stats.get(0).getLoanCount()).isEqualTo(2L);

        // Une seule requete, et elle porte l'agregat du mapping en son sein.
        assertThat(sql().selectCount()).isEqualTo(1);
        assertThat(sql().lastQuery()).contains("count(", "group by");
    }

    @Test
    @DisplayName("@Synchronize : sans lui, l'agregat lirait a cote des ecritures en attente")
    void synchronize_force_le_flush_des_tables_dont_depend_la_requete() {
        unCatalogue();

        inTransactionReturning(session -> {
            Member awa = session.createQuery(
                    "from Member m where m.fullName = 'Awa Diop'", Member.class)
                    .getSingleResult();
            Media bouts = session.createQuery(
                    "from Media m where m.title = 'Les Bouts de bois de Dieu'", Media.class)
                    .getSingleResult();

            sql().reset();

            // Emprunt cree mais PAS encore flushe : il n'est qu'en memoire.
            session.persist(new Loan(awa, bouts, LocalDate.of(2026, 4, 1)));
            assertThat(sql().insertCount()).isZero();

            MediaLoanStats stat = session.createQuery(
                    "from MediaLoanStats s where s.title = 'Les Bouts de bois de Dieu'",
                    MediaLoanStats.class).getSingleResult();

            // L'agregat voit l'emprunt. Hibernate a compris, grace au @Synchronize("loan"),
            // que cette requete depend de la table `loan`, et a donc vide ses ecritures en
            // attente AVANT de la jouer.
            assertThat(stat.getLoanCount()).isEqualTo(1L);

            // La preuve est dans l'ordre du SQL : l'INSERT precede le SELECT. Le premier
            // enonce est l'appel a la sequence, declenche par le persist lui-meme.
            List<String> emis = sql().allNormalized();
            assertThat(emis).hasSize(3);
            assertThat(emis.get(0)).isEqualTo("select nextval('loan_seq')");
            assertThat(emis.get(1)).startsWith("insert into loan");

            // Et l'on voit au passage comment le mapping est reellement execute : la requete
            // du @Subselect devient une vue inline dans le FROM.
            assertThat(emis.get(2))
                    .startsWith("select")
                    .contains("from ( select m.id as id", "group by m.id, m.title ) mls1_0");

            // Sans @Synchronize, Hibernate n'aurait vu aucune raison de flusher : le SELECT
            // serait parti seul et aurait rendu 0. C'est un resultat faux, dans la meme
            // transaction que l'ecriture qui le contredit — et rien ne l'aurait signale.
            return stat;
        });
    }
}
