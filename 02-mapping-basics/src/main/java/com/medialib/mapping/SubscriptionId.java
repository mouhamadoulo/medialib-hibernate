package com.medialib.mapping;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Classe de cle d'un {@code @IdClass}.
 *
 * <p>Contrairement a un {@link LoanKey} — qui est un {@code @Embeddable} et porte les
 * {@code @Column} — celle-ci ne porte <strong>aucune annotation de mapping</strong>. Les
 * colonnes sont declarees sur l'entite ; cette classe ne sert qu'a transporter la cle, par
 * exemple pour un {@code session.find(Subscription.class, new SubscriptionId(...))}.
 *
 * <p>Ses champs doivent porter les memes noms et les memes types que ceux annotes
 * {@code @Id} sur l'entite — c'est ce qui les apparie. Et comme toute cle, elle doit etre
 * {@link Serializable} et implementer {@code equals} / {@code hashCode} : Hibernate s'en sert
 * comme cle du contexte de persistance.
 *
 * <p>Le prix de cette syntaxe est la duplication : chaque champ de la cle existe deux fois,
 * ici et sur l'entite, et rien ne garantit qu'ils resteront d'accord.
 */
public class SubscriptionId implements Serializable {

    private Long memberId;
    private LocalDate startedOn;

    protected SubscriptionId() {
        // requis par Hibernate
    }

    public SubscriptionId(Long memberId, LocalDate startedOn) {
        this.memberId = memberId;
        this.startedOn = startedOn;
    }

    public Long getMemberId() {
        return memberId;
    }

    public LocalDate getStartedOn() {
        return startedOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubscriptionId other)) {
            return false;
        }
        return Objects.equals(memberId, other.memberId)
                && Objects.equals(startedOn, other.startedOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, startedOn);
    }
}
