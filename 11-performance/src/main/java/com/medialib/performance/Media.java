package com.medialib.performance;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

/**
 * Une oeuvre du catalogue.
 *
 * <p>{@code allocationSize = 50} : Hibernate reserve cinquante identifiants d'un coup. Sans
 * ca, insérer mille oeuvres couterait mille appels a la sequence — et le batching
 * d'insertions n'y changerait rien, puisque les allers-retours viendraient des identifiants,
 * pas des INSERT.
 */
@Entity
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_seq")
    @SequenceGenerator(name = "media_seq", sequenceName = "media_seq", allocationSize = 50)
    private Long id;

    private String title;

    private Integer loanCount = 0;

    protected Media() {
    }

    public Media(String title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getLoanCount() {
        return loanCount;
    }

    public void setLoanCount(Integer loanCount) {
        this.loanCount = loanCount;
    }
}
