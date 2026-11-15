package com.medialib.associations;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import java.time.LocalDate;

/** Une reservation en file d'attente. Ignore tout de l'agence qui la detient. */
@Entity
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reservation_seq")
    @SequenceGenerator(name = "reservation_seq", sequenceName = "reservation_seq",
            allocationSize = 1)
    private Long id;

    private LocalDate requestedOn;

    protected Reservation() {
    }

    public Reservation(LocalDate requestedOn) {
        this.requestedOn = requestedOn;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getRequestedOn() {
        return requestedOn;
    }
}
