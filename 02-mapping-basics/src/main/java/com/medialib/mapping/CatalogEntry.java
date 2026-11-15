package com.medialib.mapping;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

/**
 * Une fiche de catalogue.
 *
 * <p>Entite deliberement <strong>non annotee</strong> de {@code @Table} ni de {@code @Column} :
 * elle sert a observer ce que les strategies de nommage deduisent toutes seules. Voir
 * {@code NommageTest}.
 */
@Entity
public class CatalogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "catalog_entry_seq")
    @SequenceGenerator(name = "catalog_entry_seq", sequenceName = "catalog_entry_seq",
            allocationSize = 1)
    private Long id;

    private String shelfLocation;

    private Integer copyCount;

    protected CatalogEntry() {
        // requis par Hibernate
    }

    public CatalogEntry(String shelfLocation, Integer copyCount) {
        this.shelfLocation = shelfLocation;
        this.copyCount = copyCount;
    }

    public Long getId() {
        return id;
    }

    public String getShelfLocation() {
        return shelfLocation;
    }

    public Integer getCopyCount() {
        return copyCount;
    }
}
