package com.medialib.advanced;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Un exemplaire, rattache a une agence.
 *
 * <p><strong>{@code @Filter}</strong> : une restriction que l'on active et parametre a
 * l'execution. C'est la difference avec {@code @SQLRestriction}, qui est toujours la. Cas
 * d'usage typique : un agent d'agence ne doit voir que les exemplaires de la sienne, sans
 * qu'on ait a ajouter un {@code where} dans chaque requete du code.
 */
@Entity
@FilterDef(name = "parAgence", parameters = @ParamDef(name = "agence", type = String.class))
@Filter(name = "parAgence", condition = "branch_code = :agence")
public class Copy {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "copy_seq")
    @SequenceGenerator(name = "copy_seq", sequenceName = "copy_seq", allocationSize = 1)
    private Long id;

    private String barcode;

    private String branchCode;

    protected Copy() {
    }

    public Copy(String barcode, String branchCode) {
        this.barcode = barcode;
        this.branchCode = branchCode;
    }

    public Long getId() {
        return id;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getBranchCode() {
        return branchCode;
    }
}
