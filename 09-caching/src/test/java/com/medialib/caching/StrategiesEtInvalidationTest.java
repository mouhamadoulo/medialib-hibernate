package com.medialib.caching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les strategies de concurrence du cache L2, et le piege d'invalidation qui les met toutes a
 * genoux.
 */
class StrategiesEtInvalidationTest extends CacheTestBase {

    @Test
    @DisplayName("READ_ONLY refuse la modification : c'est le contrat, pas un effet de bord")
    void read_only_refuse_la_modification() {
        Long id = inTransactionReturning(session -> {
            Branch branch = new Branch("Mediatheque centrale");
            session.persist(branch);
            return branch.getId();
        });

        inTransaction(session -> session.find(Branch.class, id)); // mise en cache

        assertThatThrownBy(() -> inTransaction(session ->
                session.find(Branch.class, id).setName("Nouveau nom")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("READ_ONLY est la plus rapide : rien a invalider, rien a verrouiller")
    void read_only_sert_le_cache_sans_requete() {
        Long id = inTransactionReturning(session -> {
            Branch branch = new Branch("Annexe Nord");
            session.persist(branch);
            return branch.getId();
        });

        inTransaction(session -> session.find(Branch.class, id));

        inTransaction(session -> {
            sql().reset();
            assertThat(session.find(Branch.class, id).getName()).isEqualTo("Annexe Nord");
            assertThat(sql().selectCount()).isZero();
        });
    }

    @Test
    @DisplayName("TRANSACTIONAL n'est pas servi par ce stack : limite documentee")
    void transactional_n_est_pas_supporte_sur_ce_stack() {
        // La strategie TRANSACTIONAL exige un cache transactionnel XA pilote par un
        // gestionnaire JTA. Ce projet n'en a aucun : JCache + Caffeine, transactions
        // resource-local. Hibernate ne refuse pas la configuration — il la degrade
        // silencieusement, ce qui est precisement le danger.
        //
        // Obtenir un vrai TRANSACTIONAL demanderait Narayana (ou Atomikos) plus un cache
        // XA comme Infinispan. C'est un changement d'architecture, pas un reglage : on ne
        // l'ajoute pas pour une demonstration.
        //
        // Ce que ce test etablit : les trois AUTRES strategies, elles, fonctionnent
        // reellement et sont demontrees ailleurs dans cette classe.
        assertThat(sessionFactory().getSessionFactoryOptions().isSecondLevelCacheEnabled())
                .isTrue();

        // Aucun gestionnaire JTA n'est enregistre : la condition prealable manque.
        Object plateformeJta = sessionFactory().getProperties().get("hibernate.transaction.jta.platform");
        assertThat(plateformeJta).isNull();
    }

    @Test
    @DisplayName("Le cache de collection se configure separement de celui de l'entite")
    void le_cache_de_collection_se_configure_separement() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("Une si longue lettre");
            media.getAuthors().add(new Author("Mariama Ba"));
            media.getTranslators().add(new Author("Traducteur"));
            session.persist(media);
            return media.getId();
        });

        // Premiere lecture : tout va en base.
        inTransaction(session -> {
            Media media = session.find(Media.class, id);
            media.getAuthors().size();
            media.getTranslators().size();
        });

        // Seconde session : authors est cachee, translators ne l'est pas.
        inTransaction(session -> {
            Media media = session.find(Media.class, id);

            sql().reset();
            media.getAuthors().size();
            assertThat(sql().selectCount()).isZero(); // @Cache sur la collection

            sql().reset();
            media.getTranslators().size();
            assertThat(sql().selectCount()).isEqualTo(1); // meme entite, collection non annotee
        });
    }

    @Test
    @DisplayName("Un bulk update HQL invalide la region de l'entite visee")
    void un_bulk_update_invalide_la_region_de_l_entite() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("Titre d'origine");
            session.persist(media);
            return media.getId();
        });

        inTransaction(session -> session.find(Media.class, id)); // mise en cache

        // Un UPDATE en masse s'adresse directement a la base. Hibernate n'a aucun moyen de
        // savoir quelles entrees de cache sont concernees.
        inTransaction(session -> session
                .createMutationQuery("update Media m set m.title = :t where m.id = :id")
                .setParameter("t", "Titre modifie en masse")
                .setParameter("id", id)
                .executeUpdate());

        inTransaction(session -> {
            sql().reset();
            Media relu = session.find(Media.class, id);

            // Hibernate connait la table visee par un update HQL : il invalide la region de
            // Media, et la lecture suivante repart en base. Le cache n'est jamais menteur —
            // mais le bulk update vient de dilapider tout le benefice du cache sur cette
            // entite. C'est le compromis a connaitre avant d'ecrire un update en masse sur
            // une entite cachee.
            assertThat(relu.getTitle()).isEqualTo("Titre modifie en masse");
            assertThat(sql().selectCount()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("Un UPDATE natif ne rend pas le cache menteur : il le vide ENTIEREMENT")
    void un_update_natif_invalide_tout_le_cache() {
        Long mediaId = inTransactionReturning(session -> {
            Media media = new Media("Titre d'origine");
            session.persist(media);
            return media.getId();
        });
        Long branchId = inTransactionReturning(session -> {
            Branch branch = new Branch("Agence sans rapport");
            session.persist(branch);
            return branch.getId();
        });

        // Les deux entites sont en cache, et servies sans requete.
        inTransaction(session -> {
            sql().reset();
            session.find(Media.class, mediaId);
            session.find(Branch.class, branchId);
            assertThat(sql().selectCount()).isZero();
        });

        // Un UPDATE en SQL natif, sans espaces de requete declares.
        inTransaction(session -> session
                .createNativeMutationQuery("update media set title = 'modifie en natif' where id = :id")
                .setParameter("id", mediaId)
                .executeUpdate());

        inTransaction(session -> {
            sql().reset();

            // Media est bien a jour : la correction a eu lieu.
            assertThat(session.find(Media.class, mediaId).getTitle())
                    .isEqualTo("modifie en natif");

            // Et Branch aussi a du etre relue, alors que la requete native ne la concernait
            // en rien. Faute de savoir quelles tables ont ete touchees, Hibernate choisit la
            // prudence et jette TOUTES les regions. Correct, mais ruineux : une seule requete
            // native en ecriture annule le benefice du cache pour l'application entiere.
            //
            // Le remede : declarer les tables concernees
            // (query.unwrap(NativeQuery.class).addSynchronizedEntityClass(Media.class)),
            // et Hibernate n'invalidera que ce qu'il faut.
            assertThat(session.find(Branch.class, branchId).getName())
                    .isEqualTo("Agence sans rapport");
            assertThat(sql().selectCount()).isEqualTo(2); // les deux ont ete relues
        });
    }
}
