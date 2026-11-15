package com.medialib.advanced;

/**
 * Qui agit, du point de vue de l'audit.
 *
 * <p>Dans une vraie application, cette information viendrait du contexte de securite. Ici une
 * variable de thread suffit, et elle a l'avantage de montrer le mecanisme sans le cacher.
 */
public final class UtilisateurCourant {

    private static final ThreadLocal<String> COURANT = new ThreadLocal<>();

    private UtilisateurCourant() {
    }

    public static void definir(String identifiant) {
        COURANT.set(identifiant);
    }

    public static String lire() {
        String valeur = COURANT.get();
        return valeur == null ? "systeme" : valeur;
    }

    public static void effacer() {
        COURANT.remove();
    }
}
