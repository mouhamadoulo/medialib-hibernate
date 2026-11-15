package com.medialib.inheritance.tpc;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Un DVD. */
@Entity
@Table(name = "tpc_dvd")
public class TpcDvd extends TpcMedia {

    private Integer durationMinutes;

    private Integer regionCode;

    protected TpcDvd() {
    }

    public TpcDvd(String title, Integer durationMinutes, Integer regionCode) {
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
