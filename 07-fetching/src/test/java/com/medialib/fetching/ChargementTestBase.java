package com.medialib.fetching;

import com.medialib.support.HibernateTestBase;
import java.time.LocalDate;
import java.util.Map;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;

/** Les entites de ce chapitre, et un catalogue assez gros pour que le N+1 se voie. */
abstract class ChargementTestBase extends HibernateTestBase {

    /** Volumetrie des demonstrations de N+1. Cent suffit a rendre le probleme flagrant. */
    protected static final int NOMBRE_D_OEUVRES = 100;

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Media.class, Copy.class, Author.class, Branch.class, Member.class, Loan.class
        };
    }

    @Override
    protected Map<String, Object> extraSettings() {
        return Map.of(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                CamelCaseToUnderscoresNamingStrategy.class.getName());
    }

    /**
     * Cent oeuvres, chacune avec un exemplaire, un auteur, un adherent et un emprunt.
     *
     * <p>Chaque emprunt vise un adherent different : c'est ce qui empeche le cache de premier
     * niveau de masquer le N+1.
     */
    protected void centOeuvresEmpruntees() {
        inTransaction(session -> {
            Branch agence = new Branch("Mediatheque centrale");
            session.persist(agence);

            for (int i = 1; i <= NOMBRE_D_OEUVRES; i++) {
                Media media = new Media("Oeuvre " + i);
                media.addAuthor(new Author("Auteur " + i));
                Copy copy = new Copy("EX-" + i, agence);
                media.addCopy(copy);
                session.persist(media);

                Member member = new Member("Adherent " + i);
                session.persist(member);
                session.persist(new Loan(member, copy, LocalDate.of(2026, 1, 1).plusDays(i)));
            }
        });
    }
}
