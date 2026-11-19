package com.medialib.advanced;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

/**
 * Fiche d'adherent, inseree en n'ecrivant que les colonnes reellement renseignees.
 *
 * <p>Par defaut, Hibernate prepare un enonce unique par entite et y liste
 * <strong>toutes</strong> les colonnes. Un champ laisse a {@code null} part donc en NULL
 * explicite — et un defaut de colonne ne s'applique qu'en l'absence de valeur, jamais face a
 * un NULL recu.
 *
 * <p>C'est exactement ce que le chapitre 02 constate sur {@code MediaSheet} : un
 * {@code @ColumnDefault("'fr'")} y reste lettre morte et la colonne finit a {@code null}.
 * {@code @DynamicInsert} est la piece qui manquait — la colonne quitte l'enonce, et la base
 * applique enfin son defaut.
 *
 * <p>Le cout se paie cote base : l'enonce n'est plus unique, il depend de la combinaison des
 * champs renseignes. Le cache de requetes preparees du pilote et du serveur se fragmente
 * d'autant. On reserve donc l'annotation aux entites larges et majoritairement creuses, ou aux
 * cas comme celui-ci, ou le defaut de la base doit pouvoir s'exprimer.
 */
@Entity
@Table(name = "member_profile")
@DynamicInsert
public class MemberProfile {

    @Id
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @ColumnDefault("'fr'")
    @Column(length = 5)
    private String language;

    protected MemberProfile() {
    }

    public MemberProfile(Long id, String fullName, String language) {
        this.id = id;
        this.fullName = fullName;
        this.language = language;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getLanguage() {
        return language;
    }
}
