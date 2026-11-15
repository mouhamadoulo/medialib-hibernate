package com.medialib.inheritance.single;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Un DVD. */
@Entity
@DiscriminatorValue("DVD")
public class SingleDvd extends SingleMedia {

    private Integer durationMinutes;

    private Integer regionCode;

    protected SingleDvd() {
    }

    public SingleDvd(String title, Integer durationMinutes, Integer regionCode) {
        super(title);
        this.durationMinutes = durationMinutes;
        this.regionCode = regionCode;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public Integer getRegionCode() {
        return regionCode;
    }
}
