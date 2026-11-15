package com.medialib.associations;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import java.util.Objects;

/**
 * Un adherent.
 *
 * <p>equals et hashCode fondes sur une <strong>cle metier</strong> (le numero d'adherent),
 * stable des la construction et independante de la persistance. C'est la reponse au piege
 * illustre par {@link Author}.
 */
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq")
    @SequenceGenerator(name = "member_seq", sequenceName = "member_seq", allocationSize = 1)
    private Long id;

    private String membershipNumber;

    private String fullName;

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private MembershipCard card;

    protected Member() {
    }

    public Member(String membershipNumber, String fullName) {
        this.membershipNumber = membershipNumber;
        this.fullName = fullName;
    }

    public Long getId() {
        return id;
    }

    public String getMembershipNumber() {
        return membershipNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public MembershipCard getCard() {
        return card;
    }

    public void setCard(MembershipCard card) {
        this.card = card;
        card.setMember(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Member other)) {
            return false;
        }
        return Objects.equals(membershipNumber, other.membershipNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(membershipNumber); // stable des la construction
    }
}
