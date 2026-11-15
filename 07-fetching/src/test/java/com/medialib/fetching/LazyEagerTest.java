package com.medialib.fetching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.LazyInitializationException;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les defauts de JPA, et pourquoi ils sont a l'envers.
 *
 * <p>{@code @ManyToOne} et {@code @OneToOne} sont EAGER par defaut : on paie une jointure a
 * chaque chargement, meme sans en avoir besoin. {@code @OneToMany} et {@code @ManyToMany} sont
 * LAZY : on risque le N+1 et la {@code LazyInitializationException}. Autrement dit, le defaut
 * est couteux la ou on ne s'y attend pas, et piegeux ailleurs.
 */
class LazyEagerTest extends ChargementTestBase {

    @Test
    @DisplayName("find() sur une @ManyToOne EAGER : une requete, avec jointure")
    void manytoone_est_eager_par_defaut() {
        centOeuvresEmpruntees();

        Long id = inTransactionReturning(session -> session
                .createQuery("select c.id from Copy c order by c.id", Long.class)
                .setMaxResults(1).getSingleResult());

        inTransaction(session -> {
            sql().reset();

            Copy exemplaire = session.find(Copy.class, id);

            // find() connait le graphe a charger : il joint branch dans la meme requete.
            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("join");
            assertThat(Hibernate.isInitialized(exemplaire.getBranch())).isTrue();

            // media, declaree LAZY, n'est qu'un proxy.
            assertThat(Hibernate.isInitialized(exemplaire.getMedia())).isFalse();
        });
    }

    @Test
    @DisplayName("EAGER ne veut PAS dire joint : en HQL, l'association part en requete separee")
    void eager_en_hql_declenche_une_seconde_requete_au_lieu_d_une_jointure() {
        centOeuvresEmpruntees();

        inTransaction(session -> {
            sql().reset();

            Copy exemplaire = session.createQuery("select c from Copy c order by c.id", Copy.class)
                    .setMaxResults(1).getSingleResult();

            // DEUX requetes : la requete HQL telle qu'on l'a ecrite, puis une seconde pour
            // satisfaire le EAGER de branch. Hibernate n'ajoute pas de jointure a une requete
            // HQL — il respecte ce qu'on a ecrit, et comble le EAGER apres coup.
            //
            // C'est le malentendu central sur EAGER : il garantit que l'association sera
            // chargee, pas qu'elle le sera efficacement. Sur une liste de cent exemplaires
            // visant cent agences differentes, ce sont cent requetes supplementaires.
            assertThat(sql().selectCount()).isEqualTo(2);
            assertThat(Hibernate.isInitialized(exemplaire.getBranch())).isTrue();

            // La seule facon d'avoir vraiment une jointure : la demander.
            sql().reset();
            session.createQuery("select c from Copy c join fetch c.branch order by c.id",
                    Copy.class).setMaxResults(1).getSingleResult();
            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("join");
        });
    }

    @Test
    @DisplayName("@OneToMany est LAZY par defaut : la seconde requete attend qu'on y touche")
    void onetomany_est_lazy_par_defaut() {
        centOeuvresEmpruntees();

        inTransaction(session -> {
            Media media = session.createQuery("select m from Media m", Media.class)
                    .setMaxResults(1).getSingleResult();
            sql().reset();

            assertThat(Hibernate.isInitialized(media.getCopies())).isFalse();
            assertThat(sql().selectCount()).isZero();

            media.getCopies().size(); // on y touche

            assertThat(sql().selectCount()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("getReference rend un proxy : aucune requete avant le premier acces reel")
    void getreference_rend_un_proxy_non_initialise() {
        centOeuvresEmpruntees();

        Long id = inTransactionReturning(session -> session
                .createQuery("select m.id from Media m", Long.class)
                .setMaxResults(1).getSingleResult());

        inTransaction(session -> {
            sql().reset();

            Media proxy = session.getReference(Media.class, id);

            assertThat(Hibernate.isInitialized(proxy)).isFalse();
            assertThat(sql().count()).isZero();
            assertThat(proxy.getId()).isEqualTo(id); // l'identifiant, le proxy le connait
            assertThat(sql().count()).isZero();

            assertThat(proxy.getTitle()).isNotNull(); // la, il faut aller chercher

            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(Hibernate.isInitialized(proxy)).isTrue();
        });
    }

    @Test
    @DisplayName("LazyInitializationException : le proxy survit a la session, pas sa connexion")
    void acceder_hors_session_leve_lazyinitializationexception() {
        centOeuvresEmpruntees();

        // On sort une entite de sa session sans avoir touche a sa collection.
        Media detache = inTransactionReturning(session ->
                session.createQuery("select m from Media m", Media.class)
                        .setMaxResults(1).getSingleResult());

        assertThatThrownBy(() -> detache.getCopies().size())
                .isInstanceOf(LazyInitializationException.class)
                .hasMessageContaining("Cannot lazily initialize collection")
                .hasMessageContaining("no session");
    }

    @Test
    @DisplayName("La solution : charger dans la session, avant d'en sortir")
    void charger_dans_la_session_evite_l_exception() {
        centOeuvresEmpruntees();

        Media detache = inTransactionReturning(session -> {
            Media media = session.createQuery("select m from Media m join fetch m.copies",
                    Media.class).setMaxResults(1).getSingleResult();
            return media;
        });

        // Hors session, mais la collection etait deja la.
        assertThat(detache.getCopies()).hasSize(1);
    }

    @Test
    @DisplayName("Une projection ne peut pas lever cette exception : elle ne contient aucun proxy")
    void une_projection_ne_peut_pas_lever_l_exception() {
        centOeuvresEmpruntees();

        List<TitreEtAdherent> lignes = inTransactionReturning(session -> session.createQuery(
                "select new com.medialib.fetching.TitreEtAdherent(m.title, mb.fullName) "
                        + "from Loan l join l.member mb join l.copy c join c.media m",
                TitreEtAdherent.class).getResultList());

        // Hors de toute session : aucun risque, il n'y a que des chaines de caracteres.
        assertThat(lignes).hasSize(NOMBRE_D_OEUVRES);
        assertThat(lignes.get(0).titre()).startsWith("Oeuvre ");
    }
}
