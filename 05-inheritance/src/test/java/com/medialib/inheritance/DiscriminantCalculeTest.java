package com.medialib.inheritance;

import static org.assertj.core.api.Assertions.assertThat;

import com.medialib.inheritance.formula.LegacyBook;
import com.medialib.inheritance.formula.LegacyDvd;
import com.medialib.inheritance.formula.LegacyMedia;
import com.medialib.support.HibernateTestBase;
import java.util.List;
import java.util.Map;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code SINGLE_TABLE} sans colonne de discriminant : le type est <em>calcule</em> a partir des
 * donnees.
 *
 * <p>Le cas d'usage reel est la reprise d'un schema existant, qu'on n'a pas le droit de
 * modifier et qui ne prevoyait aucune colonne de type. L'information s'y trouve pourtant :
 * une ligne qui a un ISBN est un livre, une ligne qui a une duree est un DVD.
 * {@code @DiscriminatorFormula} permet de le dire a Hibernate en SQL.
 *
 * <p>Ce qui se paie, et le chapitre le montre : l'expression est evaluee a chaque ligne de
 * chaque requete, et le type d'une ligne devient une consequence de ses donnees — le modifier
 * suffit a changer la classe Java que renverra Hibernate.
 */
class DiscriminantCalculeTest extends HibernateTestBase {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {LegacyMedia.class, LegacyBook.class, LegacyDvd.class};
    }

    @Override
    protected Map<String, Object> extraSettings() {
        return Map.of(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                CamelCaseToUnderscoresNamingStrategy.class.getName());
    }

    @Test
    @DisplayName("Aucune colonne de discriminant n'est creee : le schema reste celui d'origine")
    void la_table_n_a_pas_de_colonne_de_type() {
        List<String> colonnes = inTransactionReturning(session -> session.createNativeQuery(
                "select column_name from information_schema.columns "
                        + "where table_schema = current_schema() and table_name = 'legacy_media'",
                String.class).getResultList());

        // C'est la raison d'etre de la formule : on n'ajoute rien a la table.
        assertThat(colonnes).containsExactlyInAnyOrder("id", "title", "isbn", "duration_minutes");
        assertThat(colonnes).noneMatch(c -> c.contains("type"));
    }

    @Test
    @DisplayName("La formule tranche le type, et elle apparait dans chaque requete")
    void la_formule_decide_du_type_java() {
        inTransaction(session -> {
            session.persist(new LegacyBook(1L, "Une si longue lettre", "9782723604309"));
            session.persist(new LegacyDvd(2L, "Hyenes", 110));
        });

        sql().reset();
        List<LegacyMedia> tout = inTransactionReturning(session -> session
                .createQuery("from LegacyMedia m order by m.id", LegacyMedia.class)
                .getResultList());

        // Hibernate a bien instancie deux classes differentes, sans qu'aucune colonne ne le
        // lui dise.
        assertThat(tout).hasSize(2);
        assertThat(tout.get(0)).isInstanceOf(LegacyBook.class);
        assertThat(tout.get(1)).isInstanceOf(LegacyDvd.class);

        // Et le prix est visible : l'expression est dans le SELECT, evaluee a chaque ligne.
        // Contrairement a une colonne, elle ne peut pas etre indexee telle quelle — filtrer
        // sur le type imposerait un index d'expression, que personne ne pense a creer.
        assertThat(sql().selectCount()).isEqualTo(1);
        assertThat(sql().lastQuery()).contains("case when");
    }

    @Test
    @DisplayName("Le type n'est plus une declaration mais une consequence des donnees")
    void modifier_les_donnees_change_la_classe_renvoyee() {
        inTransaction(session ->
                session.persist(new LegacyBook(3L, "Fiche incomplete", "9782266114781")));

        // On efface l'ISBN par une requete native : rien d'autre ne change dans la ligne.
        inTransaction(session -> session.createNativeMutationQuery(
                "update legacy_media set isbn = null where id = 3").executeUpdate());

        LegacyMedia relue = inTransactionReturning(session ->
                session.find(LegacyMedia.class, 3L));

        // La meme ligne est maintenant un DVD. Aucune migration, aucun avertissement : la
        // formule a simplement rendu une autre valeur.
        //
        // C'est le vrai danger de cette strategie, et il n'a pas d'equivalent avec une colonne
        // de discriminant : la moindre evolution du modele qui rend une colonne nullable
        // reclasse silencieusement des lignes existantes.
        assertThat(relue).isInstanceOf(LegacyDvd.class);
    }
}
