package com.medialib.inheritance.tpc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Hierarchie en {@code TABLE_PER_CLASS} : une table complete et autonome par sous-classe.
 *
 * <p>Chaque table repete les colonnes du parent. Charger un livre est direct, sans jointure.
 * Mais une requete polymorphe doit balayer toutes les tables avec un {@code UNION ALL}, et
 * <strong>aucune cle etrangere ne peut designer un media</strong> : il n'existe pas de table
 * ou tous les medias figurent. C'est la limite qui disqualifie souvent cette strategie.
 *
 * <p>{@code IDENTITY} est egalement impossible : les identifiants doivent etre uniques a
 * travers plusieurs tables, donc la sequence est partagee.
 */
@Entity
@Table(name = "tpc_media")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class TpcMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tpc_media_seq")
    @SequenceGenerator(name = "tpc_media_seq", sequenceName = "tpc_media_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String title;

    protected TpcMedia() {
    }

    protected TpcMedia(String title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
