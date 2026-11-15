package com.medialib.queries;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import java.time.LocalDate;

/** Un emprunt. */
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
    @JoinColumn(name = "media_id")
    private Media media;

    private LocalDate loanedOn;

    private LocalDate returnedOn;

    protected Loan() {
    }

    public Loan(Member member, Media media, LocalDate loanedOn) {
        this.member = member;
        this.media = media;
        this.loanedOn = loanedOn;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public Media getMedia() {
        return media;
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
