package com.medialib.mapping;

/**
 * Le format d'un media.
 *
 * <p>L'ordre de declaration de ces constantes est, en mapping ORDINAL, une donnee persistee.
 * Inserer un format au milieu de cette liste reinterprete silencieusement toutes les lignes
 * deja en base. Voir {@code EnumTest}.
 */
public enum MediaFormat {
    BOOK,
    DVD,
    VINYL
}
