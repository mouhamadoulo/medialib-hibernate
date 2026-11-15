package com.medialib.caching;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import java.time.LocalDate;

/**
 * Un emprunt.
 *
 * <p>Deliberement <strong>non cache</strong> : les emprunts changent sans arret, et un cache y
 * couterait plus cher qu'il ne rapporte. Le choix de ne pas cacher est aussi un choix.
 */
@Entity
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loan_seq")
    @SequenceGenerator(name = "loan_seq", sequenceName = "loan_seq", allocationSize = 1)
    private Long id;

    private LocalDate loanedOn;

    protected Loan() {
    }

    public Loan(LocalDate loanedOn) {
        this.loanedOn = loanedOn;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getLoanedOn() {
        return loanedOn;
    }
}
