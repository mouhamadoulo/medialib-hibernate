package com.medialib.mapping;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Cle composite d'un emprunt : un adherent, un exemplaire, une date.
 *
 * <p>Un {@code @EmbeddedId} doit etre {@link Serializable}, implementer {@code equals} et
 * {@code hashCode} — Hibernate s'en sert comme cle du contexte de persistance — et rester
 * immuable : changer la cle d'une entite managee la ferait disparaitre de son propre contexte.
 */
@Embeddable
public class LoanKey implements Serializable {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "copy_id", nullable = false)
    private Long copyId;

    @Column(name = "loaned_on", nullable = false)
    private LocalDate loanedOn;

    protected LoanKey() {
        // requis par Hibernate
    }

    public LoanKey(Long memberId, Long copyId, LocalDate loanedOn) {
        this.memberId = memberId;
        this.copyId = copyId;
        this.loanedOn = loanedOn;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getCopyId() {
        return copyId;
    }

    public LocalDate getLoanedOn() {
        return loanedOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LoanKey other)) {
            return false;
        }
        return Objects.equals(memberId, other.memberId)
                && Objects.equals(copyId, other.copyId)
                && Objects.equals(loanedOn, other.loanedOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, copyId, loanedOn);
    }
}
