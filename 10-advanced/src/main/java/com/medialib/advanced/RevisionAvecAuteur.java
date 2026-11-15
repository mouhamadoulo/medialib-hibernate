package com.medialib.advanced;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Date;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * L'entite de revision d'Envers, enrichie.
 *
 * <p>Par defaut, Envers n'enregistre qu'un numero de revision et un horodatage. Dans la vraie
 * vie, la question qu'on pose a un audit est « qui a fait ca ». D'ou cette entite de revision
 * personnalisee, remplie par {@link RevisionListenerAvecAuteur}.
 *
 * <p>Elle est ecrite entierement, et non derivee de {@code DefaultRevisionEntity} : cette
 * classe est {@code final} depuis Hibernate 7. Il faut donc redeclarer soi-meme les deux
 * champs obligatoires — {@code @RevisionNumber} et {@code @RevisionTimestamp}.
 */
@Entity
@Table(name = "revinfo")
@RevisionEntity(RevisionListenerAvecAuteur.class)
public class RevisionAvecAuteur {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "revinfo_seq")
    @SequenceGenerator(name = "revinfo_seq", sequenceName = "revinfo_seq", allocationSize = 1)
    @RevisionNumber
    @Column(name = "rev")
    private int id;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private long timestamp;

    @Column(name = "auteur", length = 60)
    private String auteur;

    public int getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /** L'horodatage de la revision, sous une forme utilisable. */
    public Instant getRevisionInstant() {
        return Instant.ofEpochMilli(timestamp);
    }

    public Date getRevisionDate() {
        return new Date(timestamp);
    }

    public String getAuteur() {
        return auteur;
    }

    public void setAuteur(String auteur) {
        this.auteur = auteur;
    }
}
