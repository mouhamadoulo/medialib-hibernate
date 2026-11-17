package com.medialib.mapping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Un abonnement, identifie par un adherent et une date de debut.
 *
 * <p>Meme idee que {@link Loan}, mais par l'autre syntaxe de cle composite : plusieurs
 * {@code @Id} <em>sur l'entite</em>, et une classe de cle designee par {@code @IdClass}.
 *
 * <p>Ce que cela change, concretement : {@code startedOn} et {@code memberId} sont des champs
 * de cette classe, donc adressables directement en HQL
 * ({@code where s.memberId = :m}). Avec un {@code @EmbeddedId}, il faudrait traverser la cle
 * ({@code where l.id.memberId = :m}). Le schema produit, lui, est identique.
 *
 * <p>Voir {@code CleCompositeIdClassTest}, qui met les deux cote a cote.
 */
@Entity
@Table(name = "subscription")
@IdClass(SubscriptionId.class)
public class Subscription {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Id
    @Column(name = "started_on")
    private LocalDate startedOn;

    @Column(nullable = false, length = 20)
    private String plan;

    protected Subscription() {
        // requis par Hibernate
    }

    public Subscription(Long memberId, LocalDate startedOn, String plan) {
        this.memberId = memberId;
        this.startedOn = startedOn;
        this.plan = plan;
    }

    public Long getMemberId() {
        return memberId;
    }

    public LocalDate getStartedOn() {
        return startedOn;
    }

    public String getPlan() {
        return plan;
    }
}
