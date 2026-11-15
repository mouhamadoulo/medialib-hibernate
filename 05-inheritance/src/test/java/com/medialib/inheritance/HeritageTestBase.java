package com.medialib.inheritance;

import com.medialib.support.HibernateTestBase;
import com.medialib.inheritance.joined.JoinedBook;
import com.medialib.inheritance.joined.JoinedDvd;
import com.medialib.inheritance.joined.JoinedMedia;
import com.medialib.inheritance.joined.JoinedVinyl;
import com.medialib.inheritance.mapped.Equipment;
import com.medialib.inheritance.single.SingleBook;
import com.medialib.inheritance.single.SingleDvd;
import com.medialib.inheritance.single.SingleMedia;
import com.medialib.inheritance.single.SingleVinyl;
import com.medialib.inheritance.tpc.TpcBook;
import com.medialib.inheritance.tpc.TpcDvd;
import com.medialib.inheritance.tpc.TpcMedia;
import com.medialib.inheritance.tpc.TpcVinyl;
import java.util.Map;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;

/**
 * Les <strong>trois</strong> hierarchies sont mappees dans la meme unite de persistance.
 *
 * <p>C'est volontaire et c'est tout l'interet du chapitre : les memes donnees, les memes
 * champs, trois strategies, et un comparatif possible sur le SQL emis. Les classes portent des
 * noms distincts ({@code SingleMedia}, {@code JoinedMedia}, {@code TpcMedia}) parce que deux
 * entites ne peuvent pas partager un nom dans une meme unite de persistance.
 */
abstract class HeritageTestBase extends HibernateTestBase {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            SingleMedia.class, SingleBook.class, SingleDvd.class, SingleVinyl.class,
            JoinedMedia.class, JoinedBook.class, JoinedDvd.class, JoinedVinyl.class,
            TpcMedia.class, TpcBook.class, TpcDvd.class, TpcVinyl.class,
            Equipment.class
        };
    }

    @Override
    protected Map<String, Object> extraSettings() {
        return Map.of(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                CamelCaseToUnderscoresNamingStrategy.class.getName());
    }

    /** Le meme catalogue, insere dans les trois hierarchies. */
    protected void catalogueDansLesTroisStrategies() {
        inTransaction(session -> {
            session.persist(new SingleBook("Une si longue lettre", "9782723604309", 165));
            session.persist(new SingleDvd("Hyenes", 110, 2));
            session.persist(new SingleVinyl("Pirates Choice", 2, 33));

            session.persist(new JoinedBook("Une si longue lettre", "9782723604309", 165));
            session.persist(new JoinedDvd("Hyenes", 110, 2));
            session.persist(new JoinedVinyl("Pirates Choice", 2, 33));

            session.persist(new TpcBook("Une si longue lettre", "9782723604309", 165));
            session.persist(new TpcDvd("Hyenes", 110, 2));
            session.persist(new TpcVinyl("Pirates Choice", 2, 33));
        });
    }
}
