package com.medialib.mapping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

/**
 * Une reservation, identifiee par le generateur {@code TABLE}.
 *
 * <p>Ce generateur n'utilise ni sequence ni colonne auto-incrementee : il tient le compteur
 * dans une <strong>table ordinaire</strong>, ici {@code id_generator}. Pour allouer un
 * identifiant, Hibernate lit la ligne du compteur <em>en la verrouillant</em>
 * ({@code select ... for update}), puis l'ecrit incrementee.
 *
 * <p>Soit deux allers-retours et un verrou de ligne par allocation. Sous concurrence, toutes
 * les transactions qui inserent une reservation se mettent en file derriere ce verrou — la
 * table de compteurs devient le point de contention de l'application. C'est le generateur le
 * plus lent des quatre, et il n'existe que pour les bases depourvues de sequences.
 *
 * <p>{@code allocationSize = 1} est choisi pour que le SQL soit lisible dans le test : une
 * allocation, un select, un update. En pratique on le monterait, exactement comme pour une
 * sequence, afin d'amortir le cout sur plusieurs insertions.
 */
@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "reservation_gen")
    @TableGenerator(
            name = "reservation_gen",
            table = "id_generator",
            pkColumnName = "sequence_name",
            valueColumnName = "next_val",
            pkColumnValue = "reservation",
            allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String mediaTitle;

    protected Reservation() {
        // requis par Hibernate
    }

    public Reservation(String mediaTitle) {
        this.mediaTitle = mediaTitle;
    }

    public Long getId() {
        return id;
    }

    public String getMediaTitle() {
        return mediaTitle;
    }
}
