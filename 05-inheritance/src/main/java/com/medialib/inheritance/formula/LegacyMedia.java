package com.medialib.inheritance.formula;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import org.hibernate.annotations.DiscriminatorFormula;

/**
 * Hierarchie {@code SINGLE_TABLE} dont le discriminant n'est pas une colonne mais une
 * <strong>expression SQL</strong>.
 *
 * <p>Le cas d'usage est la reprise d'un schema existant qu'on n'a pas le droit de modifier et
 * qui n'a jamais prevu de colonne de type. L'information y est pourtant presente, en creux :
 * une ligne qui porte un ISBN est un livre, une ligne qui porte une duree est un DVD.
 * {@code @DiscriminatorFormula} permet de l'enoncer en SQL plutot que d'ajouter une colonne.
 *
 * <p>Deux consequences, demontrees par {@code DiscriminantCalculeTest} :
 *
 * <ul>
 *   <li>l'expression est <strong>evaluee a chaque ligne de chaque requete</strong>, et
 *       n'etant pas une colonne, elle n'est pas indexable telle quelle — filtrer sur le type
 *       exigerait un index d'expression ;
 *   <li>le type cesse d'etre une declaration pour devenir une <strong>consequence des
 *       donnees</strong>. Effacer l'ISBN d'un livre en fait un DVD, sans migration et sans
 *       avertissement. Une colonne de discriminant n'a pas ce defaut.
 * </ul>
 *
 * <p>Hierarchie isolee des trois hierarchies principales du chapitre. L'identifiant est fourni
 * a la main, pour que le test n'ait a se preoccuper d'aucune sequence.
 */
@Entity
@Table(name = "legacy_media")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("case when isbn is not null then 'BOOK' else 'DVD' end")
public abstract class LegacyMedia {

    @Id
    private Long id;

    @Column(nullable = false)
    private String title;

    protected LegacyMedia() {
    }

    protected LegacyMedia(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
