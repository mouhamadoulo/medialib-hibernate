package com.medialib.advanced;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Un emprunt archive : ecrit une fois, jamais modifie.
 *
 * <p>{@code @Immutable} exclut l'entite du dirty checking. Hibernate n'emet donc plus aucun
 * UPDATE pour elle — et il ne <strong>refuse</strong> pas la modification, il ne la voit pas.
 * Changer un champ ne leve rien, n'ecrit rien, et laisse l'objet en memoire en desaccord avec
 * la ligne.
 *
 * <p>Ce silence est tout le danger de l'annotation, et la raison de ne la poser que sur ce qui
 * est vraiment en lecture seule : une table d'archives comme celle-ci, ou une entite mappee
 * sur une requete, qui n'a de toute facon pas de table ou ecrire (voir {@code MediaLoanStats}
 * au chapitre 06).
 *
 * <p>En echange, Hibernate peut sauter le suivi des modifications et, pour une entite cachee,
 * se passer des verrous que {@code READ_WRITE} devrait poser.
 */
@Entity
@Table(name = "archived_loan")
@Immutable
public class ArchivedLoan {

    @Id
    private Long id;

    @Column(name = "media_title", nullable = false)
    private String mediaTitle;

    protected ArchivedLoan() {
    }

    public ArchivedLoan(Long id, String mediaTitle) {
        this.id = id;
        this.mediaTitle = mediaTitle;
    }

    public Long getId() {
        return id;
    }

    public String getMediaTitle() {
        return mediaTitle;
    }

    /**
     * Presente a dessein : c'est l'existence de ce mutateur, et l'absence totale d'effet de
     * son appel, qui rend la lecon de {@code @Immutable} visible.
     */
    public void setMediaTitle(String mediaTitle) {
        this.mediaTitle = mediaTitle;
    }
}
