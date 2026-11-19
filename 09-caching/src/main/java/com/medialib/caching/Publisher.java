package com.medialib.caching;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Un editeur, cache en {@code NONSTRICT_READ_WRITE} — la quatrieme strategie du chapitre.
 *
 * <p>Son nom dit ce qu'elle abandonne, pas ce qu'elle fait. {@link Author}, en
 * {@code READ_WRITE}, pose un « soft lock » sur l'entree pendant la transaction : aucun
 * lecteur concurrent ne peut servir la valeur peremee, et apres le commit le cache contient
 * deja la nouvelle valeur.
 *
 * <p>{@code NONSTRICT_READ_WRITE} refuse de payer ce verrou. A la modification, il
 * <strong>evince</strong> simplement l'entree : la lecture suivante retourne en base, et une
 * lecture concurrente peut voir brievement l'ancienne valeur. En echange, aucune
 * synchronisation.
 *
 * <p>Le choix se decide donc sur la donnee, pas sur la performance brute : convient a ce qui
 * est lu souvent, modifie rarement, et dont une peremption de quelques millisecondes est sans
 * consequence — un catalogue d'editeurs, typiquement. A eviter des que la valeur lue sert a
 * decider d'une ecriture.
 *
 * <p>Voir {@code NonStrictEtCacheModeTest}, qui met les deux strategies cote a cote sur le
 * meme scenario.
 */
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Publisher {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "publisher_seq")
    @SequenceGenerator(name = "publisher_seq", sequenceName = "publisher_seq", allocationSize = 1)
    private Long id;

    private String name;

    protected Publisher() {
    }

    public Publisher(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
