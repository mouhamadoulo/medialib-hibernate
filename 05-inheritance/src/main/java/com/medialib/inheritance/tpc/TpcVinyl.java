package com.medialib.inheritance.tpc;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Un vinyle. */
@Entity
@Table(name = "tpc_vinyl")
public class TpcVinyl extends TpcMedia {

    private Integer sides;

    private Integer rpm;

    protected TpcVinyl() {
    }

    public TpcVinyl(String title, Integer sides, Integer rpm) {
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
