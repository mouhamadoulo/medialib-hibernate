package com.medialib.caching;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.CacheMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La quatrieme strategie de cache, et le moyen de la contourner au cas par cas.
 *
 * <p>{@code NONSTRICT_READ_WRITE} est la strategie dont on ne voit pas l'interet jusqu'a ce
 * qu'on compare son SQL a celui de {@code READ_WRITE}. Et {@code CacheMode} est le reglage
 * qu'on cherche des qu'un traitement par lot menace de chasser du cache toutes les donnees
 * utiles.
 */
class NonStrictEtCacheModeTest extends CacheTestBase {

    private Long unEditeur(String nom) {
        return inTransactionReturning(session -> {
            Publisher publisher = new Publisher(nom);
            session.persist(publisher);
            return publisher.getId();
        });
    }

    @Test
    @DisplayName("NONSTRICT_READ_WRITE : la modification EVINCE l'entree au lieu de la mettre a jour")
    void non_strict_read_write_evince_au_lieu_de_mettre_a_jour() {
        Long id = unEditeur("Presence Africaine");

        // On remplit le cache.
        inTransaction(session -> session.find(Publisher.class, id));

        inTransaction(session -> session.find(Publisher.class, id).setName("Nouvelles Editions"));

        sql().reset();
        videLesStatistiques();
        String relu = inTransactionReturning(session -> session.find(Publisher.class, id).getName());

        // La lecture retourne en base : l'entree n'est plus la. C'est toute la difference avec
        // READ_WRITE, qui aurait ecrit la nouvelle valeur dans le cache et servi un hit.
        assertThat(relu).isEqualTo("Nouvelles Editions");
        assertThat(sql().selectCount()).isEqualTo(1);
        assertThat(stats().getSecondLevelCacheMissCount()).isEqualTo(1);
        assertThat(stats().getSecondLevelCacheHitCount()).isZero();
    }

    @Test
    @DisplayName("READ_WRITE, lui, met le cache a jour : la relecture est un hit")
    void read_write_sert_la_valeur_modifiee_depuis_le_cache() {
        Long id = inTransactionReturning(session -> {
            Author author = new Author("Mariama Ba");
            session.persist(author);
            return author.getId();
        });

        inTransaction(session -> session.find(Author.class, id));
        inTransaction(session -> session.find(Author.class, id).setName("Mariama Ba (1929-1981)"));

        sql().reset();
        videLesStatistiques();
        String relu = inTransactionReturning(session -> session.find(Author.class, id).getName());

        // Aucun SQL : la valeur modifiee etait deja dans le cache.
        //
        // Le prix de ce confort est le « soft lock » que READ_WRITE doit poser pendant la
        // transaction, pour qu'aucun lecteur concurrent ne serve la valeur peremee. C'est
        // precisement ce que NONSTRICT_READ_WRITE refuse de payer : il evince, et accepte
        // qu'une lecture concurrente voie brievement l'ancienne valeur. On le choisit donc
        // pour des donnees rarement modifiees, ou une peremption breve est sans consequence.
        assertThat(relu).isEqualTo("Mariama Ba (1929-1981)");
        assertThat(sql().count()).isZero();
        assertThat(stats().getSecondLevelCacheHitCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Le CacheMode pose sur la session gouverne les requetes, mais PAS find()")
    void le_cache_mode_de_session_ne_s_applique_pas_a_find() {
        Long id = inTransactionReturning(session -> {
            Branch branch = new Branch("Mediatheque centrale");
            session.persist(branch);
            return branch.getId();
        });

        sessionFactory().getCache().evictAllRegions();
        videLesStatistiques();

        // 1. Le reglage pose sur la SESSION, puis deux find(). On s'attend a ce que le cache
        //    soit contourne. Il ne l'est pas du tout.
        for (int i = 0; i < 2; i++) {
            sessionFactory().inTransaction(session -> {
                session.setCacheMode(CacheMode.IGNORE);
                session.find(Branch.class, id);
            });
        }

        // Un miss puis un put a la premiere lecture, un hit a la seconde : exactement ce qui
        // se serait passe sans aucun CacheMode. Le reglage de session a ete ignore par find().
        //
        // C'est le piege de ce test. Ecrit ainsi, un traitement par lot croit contourner le
        // cache et le remplit en realite ligne par ligne — chassant au passage les donnees que
        // l'application utilise vraiment.
        assertThat(stats().getSecondLevelCacheMissCount()).isEqualTo(1);
        assertThat(stats().getSecondLevelCachePutCount()).isEqualTo(1);
        assertThat(stats().getSecondLevelCacheHitCount()).isEqualTo(1);

        // 2. Le meme reglage, passe cette fois a find() lui-meme. En Hibernate 7, CacheMode
        //    implemente FindOption : c'est la forme qui fonctionne.
        sessionFactory().getCache().evictAllRegions();
        videLesStatistiques();
        for (int i = 0; i < 2; i++) {
            sessionFactory().inTransaction(session ->
                    session.find(Branch.class, id, CacheMode.IGNORE));
        }

        assertThat(stats().getSecondLevelCachePutCount()).isZero();
        assertThat(stats().getSecondLevelCacheHitCount()).isZero();
        assertThat(stats().getSecondLevelCacheMissCount()).isZero();

        // 3. Et le reglage de session n'est pas inerte pour autant : il gouverne bien les
        //    requetes. L'asymetrie est la, et rien ne la signale — seul find() y echappe.
        sessionFactory().getCache().evictAllRegions();
        videLesStatistiques();
        for (int i = 0; i < 2; i++) {
            sessionFactory().inTransaction(session -> {
                session.setCacheMode(CacheMode.IGNORE);
                session.createQuery("from Branch b where b.id = :id", Branch.class)
                        .setParameter("id", id)
                        .getSingleResult();
            });
        }

        assertThat(stats().getSecondLevelCachePutCount()).isZero();
        assertThat(stats().getSecondLevelCacheHitCount()).isZero();
        assertThat(stats().getSecondLevelCacheMissCount()).isZero();

        // Preuve que l'entite EST cachable, et que les zeros ci-dessus viennent du CacheMode
        // et non d'un cache mal configure.
        videLesStatistiques();
        inTransaction(session -> session.find(Branch.class, id));

        assertThat(stats().getSecondLevelCachePutCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("CacheMode.GET : on lit le cache sans jamais l'alimenter")
    void cache_mode_get_lit_sans_ecrire() {
        Long id = inTransactionReturning(session -> {
            Branch branch = new Branch("Annexe Nord");
            session.persist(branch);
            return branch.getId();
        });

        sessionFactory().getCache().evictAllRegions();
        videLesStatistiques();

        // Forme FindOption, la seule que find() honore — voir le test precedent.
        inTransaction(session -> session.find(Branch.class, id, CacheMode.GET));

        // Le cache a ete consulte — donc un miss est compte — mais rien n'y a ete depose.
        assertThat(stats().getSecondLevelCacheMissCount()).isEqualTo(1);
        assertThat(stats().getSecondLevelCachePutCount()).isZero();

        // La lecture suivante manque encore : le cache est reste vide. C'est le reglage d'un
        // traitement qui veut profiter du cache si la donnee y est deja, sans jamais l'y
        // mettre — donc sans evincer ce que les autres y ont depose.
        videLesStatistiques();
        inTransaction(session -> session.find(Branch.class, id, CacheMode.GET));

        assertThat(stats().getSecondLevelCacheMissCount()).isEqualTo(1);
        assertThat(stats().getSecondLevelCacheHitCount()).isZero();
    }
}
