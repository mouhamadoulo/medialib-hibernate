package com.medialib.bootstrap;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Un adherent de la mediatheque.
 *
 * <p>Entite volontairement minimale : ce chapitre ne parle pas de mapping mais du cycle de vie.
 * Le generateur {@code SEQUENCE} est choisi parce qu'il permet a Hibernate de connaitre
 * l'identifiant sans emettre l'insertion — ce qui rend visible, au chapitre suivant, la
 * difference avec {@code IDENTITY}.
 */
@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq")
    @SequenceGenerator(name = "member_seq", sequenceName = "member_seq", allocationSize = 1)
    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    protected Member() {
        // requis par Hibernate
    }

    public Member(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
