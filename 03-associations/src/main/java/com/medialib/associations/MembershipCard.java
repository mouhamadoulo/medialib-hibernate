package com.medialib.associations;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import java.time.LocalDate;

/**
 * La carte d'un adherent.
 *
 * <p>{@code @MapsId} : la carte n'a pas d'identifiant a elle. Sa cle primaire <em>est</em>
 * celle de l'adherent, et sert en meme temps de cle etrangere. Une seule colonne au lieu de
 * deux, et l'unicite du un-a-un garantie par la base elle-meme, sans contrainte supplementaire.
 */
@Entity
public class MembershipCard {

    @Id
    private Long id; // ni @GeneratedValue, ni valeur a fournir : @MapsId la renseigne

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private Member member;

    private LocalDate issuedOn;

    protected MembershipCard() {
    }

    public MembershipCard(LocalDate issuedOn) {
        this.issuedOn = issuedOn;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }
}
