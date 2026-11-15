package com.medialib.fetching;

/**
 * Projection plate : le titre de l'oeuvre et le nom de l'emprunteur.
 *
 * <p>C'est la vraie reponse au N+1 dans la majorite des cas reels. Une page qui affiche une
 * liste n'a pas besoin d'entites managees : elle a besoin de deux chaines de caracteres.
 */
public record TitreEtAdherent(String titre, String adherent) {
}
