package com.medialib.advanced;

import org.hibernate.envers.RevisionListener;

/** Remplit l'auteur de chaque revision, au moment ou Envers la cree. */
public class RevisionListenerAvecAuteur implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        ((RevisionAvecAuteur) revisionEntity).setAuteur(UtilisateurCourant.lire());
    }
}
