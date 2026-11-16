package com.medialib.inheritance.strict;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Un DVD : la victime.
 *
 * <p>Il n'a pas de pages, n'en declare pas, et ne sait rien de {@link StrictBook}. C'est lui
 * qui paiera la contrainte exigee par son frere.
 */
@Entity
@DiscriminatorValue("DVD")
public class StrictDvd extends StrictMedia {

    private Integer durationMinutes;

    protected StrictDvd() {
    }

    public StrictDvd(Long id, String title, Integer durationMinutes) {
        super(id, title);
        this.durationMinutes = durationMinutes;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }
}
