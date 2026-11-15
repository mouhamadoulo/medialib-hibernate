package com.medialib.inheritance.joined;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/** Un vinyle. */
@Entity
@Table(name = "joined_vinyl")
@PrimaryKeyJoinColumn(name = "media_id")
public class JoinedVinyl extends JoinedMedia {

    private Integer sides;

    private Integer rpm;

    protected JoinedVinyl() {
    }

    public JoinedVinyl(String title, Integer sides, Integer rpm) {
        super(title);
        this.sides = sides;
        this.rpm = rpm;
    }

    public Integer getSides() {
        return sides;
    }

    public Integer getRpm() {
        return rpm;
    }
}
