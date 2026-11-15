package com.medialib.mapping;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/** Un adherent, avec son adresse aplatie dans la meme table. */
@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq")
    @SequenceGenerator(name = "member_seq", sequenceName = "member_seq", allocationSize = 1)
    private Long id;

    private String fullName;

    @Embedded
    private Address address;

    protected Member() {
        // requis par Hibernate
    }

    public Member(String fullName, Address address) {
        this.fullName = fullName;
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public Address getAddress() {
        return address;
    }
}
