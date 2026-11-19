package com.medialib.advanced;

import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;

/**
 * Compte les chargements d'oeuvres, pour rendre visible la frequence reelle de
 * {@code PostLoad}.
 *
 * <p>{@code PostLoad} est le pendant en lecture du {@link HorodatageListener} du chapitre.
 * C'est l'endroit ou enrichir une entite a son arrivee : calculer un champ derive, resoudre un
 * libelle. Et la ou l'on se trompe sur la frequence de l'appel.
 *
 * <p>Ce que {@code PostLoadTest} etablit :
 *
 * <ul>
 *   <li>l'appel a lieu <strong>par instance materialisee</strong>, pas par requete — le cout
 *       est donc proportionnel au volume ramene, ce qui impose de n'y mettre que du calcul
 *       local ;
 *   <li>il marque le passage de la ligne a l'objet, <strong>pas l'acces a l'objet</strong> :
 *       trois {@code find} dans une meme session ne declenchent qu'un appel, les suivants
 *       etant servis par le contexte de persistance. Y placer un compteur de consultations
 *       metier compterait donc les chargements, pas les lectures ;
 *   <li>rien ne deduplique au-dela de la session.
 * </ul>
 *
 * <p>Le compteur est atomique : le socle du chapitre partage sa fabrique entre les methodes
 * d'une classe de test, et rien ne garantit qu'un listener ne sera pas sollicite depuis
 * plusieurs threads.
 */
public class CompteurDeChargements implements PostLoadEventListener {

    private final AtomicInteger appels = new AtomicInteger();

    @Override
    public void onPostLoad(PostLoadEvent event) {
        if (event.getEntity() instanceof Media) {
            appels.incrementAndGet();
        }
    }

    public int nombreDAppels() {
        return appels.get();
    }

    public void reset() {
        appels.set(0);
    }
}
