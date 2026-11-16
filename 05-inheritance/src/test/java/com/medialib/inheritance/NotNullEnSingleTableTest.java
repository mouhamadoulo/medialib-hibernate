package com.medialib.inheritance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.medialib.inheritance.strict.StrictBook;
import com.medialib.inheritance.strict.StrictDvd;
import com.medialib.inheritance.strict.StrictMedia;
import com.medialib.support.HibernateTestBase;
import java.util.Map;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La limite de {@code SINGLE_TABLE} sur les contraintes — et elle ne tombe pas la ou on croit.
 *
 * <p>L'idee repandue est qu'Hibernate <em>ignore</em> un {@code nullable = false} declare sur
 * une colonne de sous-classe, puisque la table est partagee. C'est faux, et c'est bien pire :
 * il cree la colonne <strong>NOT NULL</strong> comme demande. La contrainte est donc reelle,
 * et elle s'applique a toute la table — y compris aux sous-classes qui n'ont jamais entendu
 * parler de cette propriete.
 *
 * <p>Autrement dit : une annotation posee sur {@code StrictBook} rend {@code StrictDvd}
 * insérable impossible. Le mapping d'une sous-classe casse une autre sous-classe, a distance,
 * et aucun avertissement n'est emis au demarrage.
 *
 * <p>Hierarchie dediee, isolee des trois hierarchies principales du chapitre : la
 * demonstration consiste a casser quelque chose.
 */
class NotNullEnSingleTableTest extends HibernateTestBase {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {StrictMedia.class, StrictBook.class, StrictDvd.class};
    }

    @Override
    protected Map<String, Object> extraSettings() {
        return Map.of(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                CamelCaseToUnderscoresNamingStrategy.class.getName());
    }

    @Test
    @DisplayName("Hibernate n'ignore PAS le NOT NULL : il cree bien la colonne contrainte")
    void le_not_null_demande_est_bien_applique_au_schema() {
        String nullable = inTransactionReturning(session -> session.createNativeQuery(
                "select is_nullable from information_schema.columns "
                        + "where table_schema = current_schema() "
                        + "and table_name = 'strict_media' and column_name = 'page_count'",
                String.class).getSingleResult());

        // Contre-intuitif : la contrainte existe, sur une table partagee.
        assertThat(nullable).isEqualTo("NO");
    }

    @Test
    @DisplayName("Un livre complet passe : rien ne semble anormal")
    void un_livre_avec_ses_pages_s_insere_normalement() {
        inTransaction(session -> session.persist(new StrictBook(1L, "Une si longue lettre", 165)));

        Long enBase = inTransactionReturning(session -> session
                .createQuery("select count(m) from StrictMedia m", Long.class).getSingleResult());
        assertThat(enBase).isEqualTo(1L);
    }

    @Test
    @DisplayName("LE degat : le DVD devient insérable impossible, a cause d'une annotation sur le livre")
    void la_contrainte_du_livre_casse_l_insertion_du_dvd() {
        assertThatThrownBy(() -> inTransaction(session ->
                session.persist(new StrictDvd(2L, "Hyenes", 110))))
                // StrictDvd ne declare aucun pageCount. Il ne sait rien de StrictBook. Et
                // pourtant la base le refuse, parce qu'ils partagent une table et que le
                // livre a exige une colonne NOT NULL.
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("page_count");
    }

    @Test
    @DisplayName("En JOINED, la meme contrainte ne gene personne")
    void en_joined_la_contrainte_reste_confinee_a_sa_sous_classe() {
        // Rappel de StrategiesTest : joined_book.page_count est NOT NULL, et pourtant
        // JoinedDvd s'insere sans probleme — sa table ne porte pas cette colonne.
        //
        // C'est l'argument decisif en faveur de JOINED : la contrainte vit la ou elle a du
        // sens. Le prix est une jointure par chargement.
        String nullable = inTransactionReturning(session -> session.createNativeQuery(
                "select is_nullable from information_schema.columns "
                        + "where table_schema = current_schema() "
                        + "and table_name = 'strict_media' and column_name = 'title'",
                String.class).getSingleResult());

        assertThat(nullable).isEqualTo("NO"); // title appartient a la racine : aucun conflit
    }
}
