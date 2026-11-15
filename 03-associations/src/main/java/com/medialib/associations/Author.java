package com.medialib.associations;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import java.util.Objects;

/**
 * Un auteur.
 *
 * <p><strong>equals et hashCode fondes sur l'identifiant genere</strong> : c'est le piege que
 * demontre {@code EqualsHashCodeTest}. Tant que l'entite est transient, son id est null ; des
 * que Hibernate l'attribue, le hashCode change — et l'objet devient introuvable dans le
 * {@code HashSet} ou il a ete range.
 *
 * <p>La bonne pratique est dans {@link Member} : une cle metier stable, ou un hashCode
 * constant.
 */
@Entity
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "author_seq")
    @SequenceGenerator(name = "author_seq", sequenceName = "author_seq", allocationSize = 1)
    private Long id;

    private String name;

    protected Author() {
    }

    public Author(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Author other)) {
            return false;
        }
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id); // change quand Hibernate attribue l'id
    }
}
