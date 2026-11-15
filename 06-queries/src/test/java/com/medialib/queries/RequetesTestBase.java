package com.medialib.queries;

import com.medialib.support.HibernateTestBase;
import java.time.LocalDate;
import java.util.Map;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;

/** Les entites de ce chapitre, et un catalogue commun a tous ses tests. */
abstract class RequetesTestBase extends HibernateTestBase {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {Media.class, Author.class, Member.class, Loan.class};
    }

    @Override
    protected Map<String, Object> extraSettings() {
        return Map.of(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                CamelCaseToUnderscoresNamingStrategy.class.getName());
    }

    /** Trois oeuvres, trois auteurs, deux adherents, trois emprunts. */
    protected void unCatalogue() {
        inTransaction(session -> {
            Author ba = new Author("Mariama Ba", "SN");
            Author kane = new Author("Cheikh Hamidou Kane", "SN");
            Author sembene = new Author("Ousmane Sembene", "SN");

            Media lettre = new Media("Une si longue lettre", 1979);
            lettre.addAuthor(ba);
            Media aventure = new Media("L'Aventure ambigue", 1961);
            aventure.addAuthor(kane);
            Media bouts = new Media("Les Bouts de bois de Dieu", 1960);
            bouts.addAuthor(sembene);

            session.persist(ba);
            session.persist(kane);
            session.persist(sembene);
            session.persist(lettre);
            session.persist(aventure);
            session.persist(bouts);

            Member awa = new Member("Awa Diop", "Dakar");
            Member ibrahima = new Member("Ibrahima Fall", "Saint-Louis");
            session.persist(awa);
            session.persist(ibrahima);

            session.persist(new Loan(awa, lettre, LocalDate.of(2026, 1, 10)));
            session.persist(new Loan(awa, aventure, LocalDate.of(2026, 2, 14)));
            session.persist(new Loan(ibrahima, lettre, LocalDate.of(2026, 3, 1)));
        });
    }
}
