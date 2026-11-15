package com.medialib.advanced;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import java.time.LocalDate;
import org.hibernate.envers.Audited;

/**
 * Un emprunt, historise par Envers.
 *
 * <p>{@code @Audited} suffit : Envers cree une table {@code loan_aud}, y ecrit une ligne a
 * chaque modification, et relie chaque ligne a une revision. L'historique devient
 * interrogeable sans qu'une seule ligne de code metier ne s'en occupe.
 */
@Entity
@Audited
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loan_seq")
    @SequenceGenerator(name = "loan_seq", sequenceName = "loan_seq", allocationSize = 1)
    private Long id;

    private String memberName;

    private LocalDate loanedOn;

    private LocalDate returnedOn;

    protected Loan() {
    }

    public Loan(String memberName, LocalDate loanedOn) {
        this.memberName = memberName;
        this.loanedOn = loanedOn;
    }

    public Long getId() {
        return id;
    }

    public String getMemberName() {
        return memberName;
    }

    public LocalDate getLoanedOn() {
        return loanedOn;
    }

    public LocalDate getReturnedOn() {
        return returnedOn;
    }

    public void setReturnedOn(LocalDate returnedOn) {
        this.returnedOn = returnedOn;
    }
}
