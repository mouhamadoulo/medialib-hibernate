package com.medialib.advanced;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;

/**
 * Une amende dont les ordres d'ecriture sont ecrits a la main.
 *
 * <p>{@code @SQLInsert} et {@code @SQLUpdate} ne modifient pas le SQL genere : ils le
 * <strong>remplacent</strong>. Hibernate emet la chaine telle quelle et se contente d'y lier
 * ses parametres. Le {@code upper()} ci-dessous le prouve — son SQL genere ne l'aurait jamais
 * produit.
 *
 * <p>Les cas d'usage reels sont ceux qu'Hibernate ne sait pas exprimer : un
 * {@code on conflict} de PostgreSQL, l'appel d'une fonction, l'ecriture dans une colonne d'un
 * type que le mapping ne couvre pas.
 *
 * <p><strong>Le piege est l'ordre des parametres.</strong> Il n'est pas celui de la
 * declaration des champs : c'est celui qu'Hibernate utilise pour son propre enonce, les
 * colonnes de donnees d'abord, l'identifiant ensuite. Ajouter un champ a l'entite sans
 * retoucher la chaine decale silencieusement toutes les liaisons — rien ne le signale a la
 * compilation, et la base se remplit de travers.
 *
 * <p>L'identifiant est assigne a la main : une sequence ajouterait un parametre a gerer dans
 * des chaines dont le propos n'est pas la generation de cle.
 */
@Entity
@Table(name = "fine")
@SQLInsert(sql = "insert into fine (amount, reason, id) values (?, upper(?), ?)")
@SQLUpdate(sql = "update fine set amount = ?, reason = upper(?) where id = ?")
public class Fine {

    @Id
    private Long id;

    @Column(precision = 8, scale = 2)
    private BigDecimal amount;

    private String reason;

    protected Fine() {
    }

    public Fine(Long id, BigDecimal amount, String reason) {
        this.id = id;
        this.amount = amount;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
