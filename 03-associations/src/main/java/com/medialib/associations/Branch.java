package com.medialib.associations;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import java.util.ArrayList;
import java.util.List;

/**
 * Une agence du reseau.
 *
 * <p>Sa liste de reservations est un {@code @OneToMany} <strong>unidirectionnel</strong> :
 * cote Branch seulement, sans {@code mappedBy}. C'est le cas que la plupart des developpeurs
 * ecrivent spontanement, et c'est celui qui coute le plus cher — faute de cote proprietaire du
 * cote « plusieurs », Hibernate n'a nulle part ou ecrire la cle etrangere et fabrique une
 * table de jonction. Voir {@code OneToManyTest}.
 */
@Entity
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "branch_seq")
    @SequenceGenerator(name = "branch_seq", sequenceName = "branch_seq", allocationSize = 1)
    private Long id;

    private String name;

    @OneToMany(cascade = CascadeType.ALL)
    private List<Reservation> reservations = new ArrayList<>();

    protected Branch() {
    }

    public Branch(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }
}
