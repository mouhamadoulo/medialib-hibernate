package com.medialib.inheritance.joined;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/** Un DVD. */
@Entity
@Table(name = "joined_dvd")
@PrimaryKeyJoinColumn(name = "media_id")
public class JoinedDvd extends JoinedMedia {

    private Integer durationMinutes;

    private Integer regionCode;

    protected JoinedDvd() {
    }

    public JoinedDvd(String title, Integer durationMinutes, Integer regionCode) {
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
