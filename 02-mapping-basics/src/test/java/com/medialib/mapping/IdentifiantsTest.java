package com.medialib.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le choix du generateur d'identifiant n'est pas un detail : il decide du moment ou Hibernate
 * doit ecrire, donc de sa capacite a differer et regrouper les insertions.
 */
class IdentifiantsTest extends MappingTestBase {

    @Test
    @DisplayName("IDENTITY force l'INSERT des le persist : plus rien n'est differable")
    void identity_insere_immediatement_et_interdit_tout_regroupement() {
        inTransaction(session -> {
            sql().reset();

            session.persist(new Publisher("Presence Africaine"));
            session.persist(new Publisher("Gallimard"));
            session.persist(new Publisher("Nouvelles Editions Africaines"));

            // Trois INSERT deja partis, avant meme le flush : la base seule connait la cle.
            assertThat(sql().insertCount()).isEqualTo(3);
        });
    }

    @Test
    @DisplayName("SEQUENCE connait la cle d'avance : les INSERT attendent le flush")
    void sequence_differe_les_insertions_jusqu_au_flush() {
        inTransaction(session -> {
            sql().reset();

            session.persist(new Media("Une si longue lettre", new Isbn("978-2-7236-0430-9"),
                    MediaFormat.BOOK, 165));
            session.persist(new Media("L'Aventure ambigue", new Isbn("978-2-264-03061-9"),
                    MediaFormat.BOOK, 191));
            session.persist(new Media("Les Bouts de bois de Dieu", new Isbn("978-2-266-11478-1"),
                    MediaFormat.BOOK, 379));

            // Les sequences ont ete interrogees, mais rien n'est encore ecrit.
            assertThat(sql().insertCount()).isZero();
            assertThat(sql().selectCount()).isEqualTo(3); // nextval, une fois par entite

            session.flush();

            assertThat(sql().insertCount()).isEqualTo(3);
        });
    }

    @Test
    @DisplayName("TABLE : l'allocation se fait hors de la session, le compteur ne la voit pas")
    void table_alloue_dans_sa_propre_transaction() {
        long avant = valeurDuCompteurDeReservations();

        inTransaction(session -> {
            sql().reset();

            Reservation reservation = new Reservation("Une si longue lettre");
            session.persist(reservation);

            assertThat(reservation.getId()).isNotNull();

            // Le fait marquant, et il est contre-intuitif : le compteur SQL de ce depot est un
            // StatementInspector de session, et il ne voit RIEN. Le generateur TABLE n'alloue
            // pas sur la connexion de la session — il en prend une a lui et valide son
            // increment dans une transaction separee. Il faut qu'il en soit ainsi : si
            // l'allocation etait annulee avec la transaction applicative, deux transactions
            // concurrentes repartiraient avec le meme identifiant.
            assertThat(sql().count()).isZero();

            session.flush();

            // L'INSERT, lui, repasse par la session, donc par le compteur. Comme SEQUENCE et
            // contrairement a IDENTITY, il a pu attendre le flush.
            assertThat(sql().insertCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("insert into reservation");
        });

        // La preuve du mecanisme est dans la table d'appoint : son compteur a avance. C'est ce
        // select-puis-update sur une ligne verrouillee qui rend ce generateur le plus lent des
        // quatre — sous concurrence, toutes les insertions font la queue derriere ce verrou.
        // Il n'existe que pour les bases depourvues de sequences.
        assertThat(valeurDuCompteurDeReservations()).isGreaterThan(avant);
    }

    /** Le compteur que le generateur TABLE tient dans une table ordinaire. */
    private long valeurDuCompteurDeReservations() {
        return inTransactionReturning(session -> session.createNativeQuery(
                "select next_val from id_generator where sequence_name = 'reservation'",
                Long.class).getSingleResult());
    }

    @Test
    @DisplayName("AUTO ne veut pas dire IDENTITY : sur PostgreSQL, Hibernate choisit une sequence")
    void auto_se_resout_en_sequence_sur_postgresql() {
        // Le malentendu courant : AUTO = « laisse la base faire », donc une colonne
        // auto-incrementee. Sur PostgreSQL avec Hibernate 7, AUTO se resout en SEQUENCE — et
        // la sequence porte un nom derive de l'entite.
        String sequence = inTransactionReturning(session -> session.createNativeQuery(
                "select sequence_name from information_schema.sequences "
                        + "where sequence_schema = current_schema() "
                        + "and sequence_name like 'author%'",
                String.class).getSingleResult());
        assertThat(sequence).isNotNull();

        inTransaction(session -> {
            sql().reset();

            session.persist(new Author("Mariama Ba"));
            session.persist(new Author("Cheikh Hamidou Kane"));
            session.persist(new Author("Ousmane Sembene"));

            // Deux appels a la sequence pour trois entites, et c'est bien une sequence
            // PostgreSQL qui est interrogee, pas une colonne auto-incrementee.
            assertThat(sql().allNormalized()).containsOnly("select nextval('author_seq')");
            assertThat(sql().selectCount()).isEqualTo(2);

            // Deux et non un : l'optimiseur « pooled » d'Hibernate appelle la sequence une
            // premiere fois pour connaitre la borne basse, une seconde pour reserver la borne
            // haute. Apres quoi il sert les identifiants de memoire.
            assertThat(sql().insertCount()).isZero();

            session.flush();
            assertThat(sql().insertCount()).isEqualTo(3);

            // La preuve que le bloc est bien reserve : vingt entites de plus, et la sequence
            // n'est pas rappelee. L'allocationSize par defaut vaut 50.
            sql().reset();
            for (int i = 0; i < 20; i++) {
                session.persist(new Author("Auteur " + i));
            }

            assertThat(sql().selectCount()).isZero();

            // Si AUTO s'etait resolu en IDENTITY, ces vingt entites auraient deja provoque
            // vingt INSERT immediats, et aucun regroupement n'aurait ete possible.
            assertThat(sql().insertCount()).isZero();
        });
    }

    @Test
    @DisplayName("UUID : l'identifiant existe avant tout dialogue avec la base")
    void uuid_genere_un_identifiant_cote_java() {
        inTransaction(session -> {
            Fine fine = new Fine(Money.ofEuros("2.50"), "retard de 5 jours");
            sql().reset();

            session.persist(fine);

            assertThat(fine.getId()).isNotNull();
            assertThat(sql().count()).isZero(); // aucun aller-retour : ni sequence, ni insert
        });
    }
}
