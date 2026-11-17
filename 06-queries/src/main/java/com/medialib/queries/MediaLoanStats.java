package com.medialib.queries;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

/**
 * Nombre d'emprunts par oeuvre — une entite mappee sur une requete, pas sur une table.
 *
 * <p>Rien n'est cree en base : l'expression de {@code @Subselect} est injectee comme
 * sous-requete a chaque interrogation. On obtient donc une vue sans avoir a en creer une, ce
 * qui evite aussi de la maintenir dans les migrations.
 *
 * <p>{@code @Immutable} n'est pas decoratif : une entite de ce type n'a pas de table ou ecrire,
 * et sans lui Hibernate tenterait des UPDATE au flush des qu'un champ change. Le chapitre 10
 * reprend cette annotation pour elle-meme, sur une entite qui, elle, aurait pu etre modifiee.
 *
 * <p><strong>{@code @Synchronize} est l'annotation qui compte ici.</strong> Hibernate ne lit
 * pas l'expression SQL pour en deduire les tables dont elle depend : il ne sait donc pas qu'il
 * doit vider ses ecritures en attente avant de la jouer. Sans cette liste, un emprunt cree
 * mais pas encore flushe serait invisible de l'agregat — un resultat faux, dans la transaction
 * meme qui contient l'ecriture qui le contredit, et sans le moindre avertissement.
 *
 * <p>Le {@code left join} est delibere : une oeuvre jamais empruntee doit apparaitre avec un
 * compte de zero, et non disparaitre de la statistique.
 */
@Entity
@Immutable
@Subselect("""
        select m.id as id,
               m.title as title,
               count(l.id) as loan_count
        from media m
        left join loan l on l.media_id = m.id
        group by m.id, m.title
        """)
@Synchronize({"media", "loan"})
public class MediaLoanStats {

    @Id
    private Long id;

    private String title;

    @Column(name = "loan_count")
    private Long loanCount;

    protected MediaLoanStats() {
        // requis par Hibernate
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Long getLoanCount() {
        return loanCount;
    }
}
