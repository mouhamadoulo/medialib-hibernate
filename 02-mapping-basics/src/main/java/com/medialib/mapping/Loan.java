package com.medialib.mapping;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Un emprunt, identifie par sa cle composite {@link LoanKey}. */
@Entity
@Table(name = "loan")
public class Loan {

    @EmbeddedId
    private LoanKey id;

    private LocalDate dueOn;

    protected Loan() {
        // requis par Hibernate
    }

    public Loan(LoanKey id, LocalDate dueOn) {
        this.id = id;
        this.dueOn = dueOn;
    }

    public LoanKey getId() {
        return id;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }
}
