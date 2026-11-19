package com.medialib.advanced;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code PostLoad} : le pendant en lecture du {@code PreInsert} du chapitre.
 *
 * <p>C'est l'endroit ou l'on enrichit une entite a son arrivee — calculer un champ derive,
 * resoudre un libelle, horodater une consultation. Et c'est l'endroit ou l'on se trompe sur
 * la frequence de l'appel.
 *
 * <p>Le listener est enregistre pour cette seule classe de test : l'ajouter au socle du
 * chapitre ferait compter des chargements dans tous les autres tests.
 */
class PostLoadTest extends AvanceTestBase {

    private final CompteurDeChargements compteur = new CompteurDeChargements();

    @Override
    protected void apresDemarrage(SessionFactory sessionFactory) {
        super.apresDemarrage(sessionFactory);

        EventListenerRegistry registre = sessionFactory
                .unwrap(SessionFactoryImplementor.class)
                .getEventEngine()
                .getListenerRegistry();
        registre.appendListeners(EventType.POST_LOAD, compteur);
    }

    @BeforeEach
    void remetLeCompteurAZero() {
        compteur.reset();
    }

    private void troisOeuvres() {
        inTransaction(session -> {
            session.persist(new Media("9782723604309", "Une si longue lettre"));
            session.persist(new Media("9782264030619", "L'Aventure ambigue"));
            session.persist(new Media("9782266114781", "Les Bouts de bois de Dieu"));
        });
    }

    @Test
    @DisplayName("PostLoad se declenche une fois par entite reellement chargee")
    void post_load_est_appele_une_fois_par_entite_chargee() {
        troisOeuvres();
        compteur.reset();

        sql().reset();
        List<Media> tout = inTransactionReturning(session -> session
                .createQuery("from Media m order by m.id", Media.class).getResultList());

        assertThat(tout).hasSize(3);
        assertThat(sql().selectCount()).isEqualTo(1);

        // Une requete, trois entites materialisees, trois appels. Le listener n'est donc pas
        // appele par requete mais par instance — le cout est proportionnel au volume ramene,
        // ce qui est la raison de n'y mettre que du calcul local.
        assertThat(compteur.nombreDAppels()).isEqualTo(3);
    }

    @Test
    @DisplayName("Ni au second acces, ni meme au second find : le cache de session l'evite")
    void post_load_ne_se_declenche_pas_sur_une_entite_deja_en_session() {
        troisOeuvres();
        Long id = inTransactionReturning(session -> session
                .createQuery("select m.id from Media m order by m.id", Long.class)
                .setMaxResults(1).getSingleResult());

        compteur.reset();
        inTransaction(session -> {
            session.find(Media.class, id);
            session.find(Media.class, id);
            session.find(Media.class, id);
        });

        // Trois find, un seul chargement : les deux suivants sont servis par le contexte de
        // persistance, et PostLoad ne s'y declenche pas.
        //
        // L'evenement marque donc le passage de la ligne a l'objet, pas l'acces a l'objet. Y
        // placer un compteur de consultations metier serait une erreur : il compterait les
        // chargements, pas les lectures.
        assertThat(compteur.nombreDAppels()).isEqualTo(1);
    }

    @Test
    @DisplayName("Il se redeclenche dans une nouvelle session, meme ligne")
    void post_load_se_redeclenche_a_chaque_session() {
        troisOeuvres();
        Long id = inTransactionReturning(session -> session
                .createQuery("select m.id from Media m order by m.id", Long.class)
                .setMaxResults(1).getSingleResult());

        compteur.reset();
        for (int i = 0; i < 3; i++) {
            inTransaction(session -> session.find(Media.class, id));
        }

        // Trois sessions, trois chargements, trois appels. Le contexte de persistance meurt
        // avec sa session : rien ne deduplique au-dela.
        assertThat(compteur.nombreDAppels()).isEqualTo(3);
    }
}
