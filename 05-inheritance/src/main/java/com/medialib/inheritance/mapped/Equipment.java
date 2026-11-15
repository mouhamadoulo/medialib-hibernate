package com.medialib.inheritance.mapped;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;

/** Du materiel de l'agence : liseuse, tourne-disque, poste informatique. */
@Entity
@Table(name = "equipment")
public class Equipment extends AbstractAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "equipment_seq")
    @SequenceGenerator(name = "equipment_seq", sequenceName = "equipment_seq", allocationSize = 1)
    private Long id;

    private String label;

    protected Equipment() {
    }

    public Equipment(String label, Instant createdAt, String createdBy) {
        super(createdAt, createdBy);
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
