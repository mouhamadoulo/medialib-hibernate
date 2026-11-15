package com.medialib.inheritance.single;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Un vinyle. */
@Entity
@DiscriminatorValue("VINYL")
public class SingleVinyl extends SingleMedia {

    private Integer sides;

    private Integer rpm;

    protected SingleVinyl() {
    }

    public SingleVinyl(String title, Integer sides, Integer rpm) {
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
