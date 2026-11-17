package com.medialib.inheritance.formula;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Un DVD du schema repris — c'est-a-dire, pour la formule, toute ligne sans ISBN.
 *
 * <p>La definition est negative, et c'est exactement ce qui rend la strategie fragile : une
 * ligne mal remplie n'est pas rejetee, elle devient un DVD.
 */
@Entity
@DiscriminatorValue("DVD")
public class LegacyDvd extends LegacyMedia {

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    protected LegacyDvd() {
    }

    public LegacyDvd(Long id, String title, Integer durationMinutes) {
        super(id, title);
        this.durationMinutes = durationMinutes;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }
}
