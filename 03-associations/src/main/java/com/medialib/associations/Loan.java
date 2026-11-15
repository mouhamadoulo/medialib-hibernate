package com.medialib.associations;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import java.time.LocalDate;

/**
 * Un emprunt.
 *
 * <p>C'est une <strong>entite d'association</strong> : elle relie un adherent et un exemplaire,
 * et porte en plus la date d'emprunt. Des qu'une relation « plusieurs a plusieurs » gagne le
 * moindre attribut, le {@code @ManyToMany} ne suffit plus — on ne peut rien ranger dans une
 * table de jonction qu'Hibernate gere seul. Deux {@code @ManyToOne} et l'affaire est reglee.
 */
@Entity
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loan_seq")
    @SequenceGenerator(name = "loan_seq", sequenceName = "loan_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "copy_id")
    private Copy copy;

    private LocalDate loanedOn;

    protected Loan() {
    }

    public Loan(Member member, Copy copy, LocalDate loanedOn) {
        this.member = member;
        this.copy = copy;
        this.loanedOn = loanedOn;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public Copy getCopy() {
        return copy;
    }

    public LocalDate getLoanedOn() {
        return loanedOn;
    }
}
