package com.medialib.mapping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Une amende de retard.
 *
 * <p>Identifiant en {@code UUID} : genere cote Java, donc connu avant toute ecriture. C'est le
 * seul generateur qui permet de construire un graphe d'objets complet sans dialoguer avec la
 * base — au prix d'une cle de 16 octets et d'index moins compacts.
 */
@Entity
@Table(name = "fine")
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "amount_cents", nullable = false)
    private Money amount; // converti en centimes par MoneyConverter (autoApply)

    private String reason;

    protected Fine() {
        // requis par Hibernate
    }

    public Fine(Money amount, String reason) {
        this.amount = amount;
        this.reason = reason;
    }

    public UUID getId() {
        return id;
    }

    public Money getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }
}
