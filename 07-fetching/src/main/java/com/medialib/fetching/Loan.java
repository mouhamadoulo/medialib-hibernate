package com.medialib.fetching;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.SequenceGenerator;
import java.time.LocalDate;

/**
 * Un emprunt.
 *
 * <p>Porte un {@code @NamedEntityGraph} : une facon declarative de dire « quand tu charges un
 * emprunt par ce graphe, ramene aussi l'adherent et l'oeuvre de l'exemplaire ». A la difference
 * du {@code join fetch}, le graphe n'est pas inscrit dans la requete : la meme requete peut
 * etre executee avec ou sans lui.
 */
@Entity
@NamedEntityGraph(name = "Loan.avecAdherentEtOeuvre",
        attributeNodes = {
            @NamedAttributeNode("member"),
            @NamedAttributeNode("copy")
        })
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
