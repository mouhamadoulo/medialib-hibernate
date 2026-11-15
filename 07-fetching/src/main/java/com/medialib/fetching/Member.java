package com.medialib.fetching;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

/**
 * Un adherent.
 *
 * <p>Deliberement <strong>sans</strong> {@code @BatchSize}, contrairement a {@link Media} :
 * c'est le temoin qui rend le N+1 visible. Voir {@code NPlusUnTest}.
 */
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq")
    @SequenceGenerator(name = "member_seq", sequenceName = "member_seq", allocationSize = 1)
    private Long id;

    private String fullName;

    protected Member() {
    }

    public Member(String fullName) {
        this.fullName = fullName;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }
}
