package com.medialib.advanced;

import java.time.Instant;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Horodate automatiquement toute oeuvre inseree.
 *
 * <p>Un listener d'evenements s'interpose dans le cycle de vie d'Hibernate lui-meme. Son
 * interet par rapport a un appel dans le code metier : il est <strong>impossible a oublier</strong>.
 *
 * <p>Le piege a connaitre : modifier l'entite dans un {@code PreInsert} ne suffit pas. L'INSERT
 * est deja construit, sous forme d'un tableau d'etat. Il faut ecrire dans
 * {@code event.getState()}, a l'index de la propriete — sinon le champ reste nul en base tout
 * en paraissant rempli en memoire.
 */
public class HorodatageListener implements PreInsertEventListener {

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        if (event.getEntity() instanceof Media media) {
            Instant maintenant = Instant.now();
            media.setCreatedAt(maintenant);
            ecrisDansLEtat(event, "createdAt", maintenant);
        }
        return false; // false = on ne veut pas annuler l'insertion
    }

    private static void ecrisDansLEtat(PreInsertEvent event, String propriete, Object valeur) {
        EntityPersister persister = event.getPersister();
        String[] proprietes = persister.getPropertyNames();
        for (int i = 0; i < proprietes.length; i++) {
            if (proprietes[i].equals(propriete)) {
                event.getState()[i] = valeur;
                return;
            }
        }
    }
}
