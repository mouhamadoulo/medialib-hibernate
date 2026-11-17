package com.medialib.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ce que la colonne doit a la base, et ce qu'elle doit a Hibernate.
 *
 * <p>Quatre annotations qui ont l'air de se ressembler et qui agissent a des moments
 * radicalement differents : {@code @Lob} choisit un type SQL, {@code @ColumnDefault} et
 * {@code @Check} n'ecrivent que du DDL, {@code @Generated} change le SQL de chaque insertion.
 */
class ColonnesEtContraintesTest extends MappingTestBase {

    @Test
    @DisplayName("@Lob sur une String ne donne pas `text` : PostgreSQL recoit un `oid`")
    void lob_choisit_un_type_sql_dedie() {
        String type = typeDeColonne("media_sheet", "full_text");

        // L'attente naturelle est `text`, le type texte illimite de PostgreSQL. Ce n'est pas ce
        // qu'on obtient : @Lob demande un *large object*, et sur PostgreSQL un large object est
        // stocke hors de la table, dans le catalogue `pg_largeobject`. La colonne ne contient
        // qu'un `oid`, c'est-a-dire un pointeur.
        //
        // Les consequences sont lourdes et passent inapercues jusqu'a la production : la valeur
        // n'est plus lisible par un `select` ordinaire, elle echappe a `pg_dump` sans l'option
        // adequate, et supprimer la ligne ne supprime pas l'objet pointe — il faut le ramasser
        // explicitement.
        //
        // Pour un texte long qu'on veut simplement illimite et dans la table, @Lob est le
        // mauvais outil : il faut @JdbcTypeCode(SqlTypes.LONG32VARCHAR), ou un
        // columnDefinition = "text".
        assertThat(type).isEqualTo("oid");

        // La colonne sans @Lob reste un varchar borne, elle.
        assertThat(typeDeColonne("media_sheet", "language")).isEqualTo("character varying");
    }

    @Test
    @DisplayName("@ColumnDefault n'ecrit que du DDL : un null Java ecrase le defaut de la base")
    void column_default_est_ignore_des_que_hibernate_ecrit_la_colonne() {
        // Le defaut existe bien dans le schema.
        assertThat(defautDeColonne("media_sheet", "language")).contains("'fr'");

        inTransaction(session -> {
            sql().reset();
            session.persist(new MediaSheet("Une si longue lettre", "texte integral", null, 165));
            session.flush();

            // Le piege : Hibernate liste TOUTES les colonnes dans son INSERT, y compris celles
            // laissees a null. La base recoit donc un NULL explicite, et un defaut de colonne
            // ne s'applique qu'en l'absence de valeur.
            assertThat(sql().lastQuery()).contains("language");
        });

        String langue = inTransactionReturning(session -> session.createNativeQuery(
                "select language from media_sheet", String.class).getSingleResult());

        // Pas 'fr'. Le defaut n'a jamais eu sa chance.
        assertThat(langue).isNull();
    }

    @Test
    @DisplayName("@Generated : la colonne quitte l'INSERT et revient par un `returning`")
    void generated_laisse_la_base_calculer_puis_relit() {
        inTransaction(session -> {
            MediaSheet fiche = new MediaSheet("L'Aventure ambigue", "texte integral", "fr", 191);
            sql().reset();

            session.persist(fiche);
            session.flush();

            // Tout le chapitre tient dans cette seule requete. La colonne calculee est absente
            // de la liste des colonnes inserees — c'est ce qui laisse enfin le @ColumnDefault
            // s'appliquer, la ou le test precedent le voyait ecrase par un NULL explicite.
            assertThat(sql().allNormalized()).containsExactly(
                    "select nextval('media_sheet_seq')",
                    "insert into media_sheet (full_text,language,page_count,title,id) "
                            + "values (?,?,?,?,?) returning indexed_on");

            // Et la valeur revient par la clause `returning` de l'INSERT, pas par un SELECT
            // separe : sur un dialecte qui sait faire, une colonne generee ne coute AUCUN
            // aller-retour supplementaire. Le seul select visible est l'appel a la sequence.
            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().insertCount()).isEqualTo(1);

            // L'objet en memoire est d'accord avec la ligne sans qu'on ait eu a le rafraichir.
            assertThat(fiche.getIndexedOn()).isEqualTo(LocalDate.now());
        });
    }

    @Test
    @DisplayName("@Check : la contrainte vit en base, et c'est la base qui refuse")
    void check_est_une_contrainte_de_schema() {
        assertThat(contraintesCheckDe("media_sheet")).anyMatch(c -> c.contains("page_count"));

        // Hibernate ne verifie rien cote Java ici — contrairement a nullable = false, qui lui
        // fait lever PropertyValueException avant l'INSERT (voir le chapitre 05). Un @Check
        // n'existe que dans le schema : l'INSERT part, et PostgreSQL le rejette.
        assertThatThrownBy(() -> inTransaction(session -> {
            session.persist(new MediaSheet("Pagination absurde", "texte", "fr", -3));
            session.flush();
        })).isInstanceOf(ConstraintViolationException.class);
    }

    private String typeDeColonne(String table, String colonne) {
        return inTransactionReturning(session -> session.createNativeQuery(
                "select data_type from information_schema.columns "
                        + "where table_schema = current_schema() "
                        + "and table_name = :table and column_name = :colonne",
                String.class)
                .setParameter("table", table)
                .setParameter("colonne", colonne)
                .getSingleResult());
    }

    private String defautDeColonne(String table, String colonne) {
        return inTransactionReturning(session -> session.createNativeQuery(
                "select column_default from information_schema.columns "
                        + "where table_schema = current_schema() "
                        + "and table_name = :table and column_name = :colonne",
                String.class)
                .setParameter("table", table)
                .setParameter("colonne", colonne)
                .getSingleResult());
    }

    private java.util.List<String> contraintesCheckDe(String table) {
        return inTransactionReturning(session -> session.createNativeQuery(
                "select cc.check_clause from information_schema.check_constraints cc "
                        + "join information_schema.table_constraints tc "
                        + "  on tc.constraint_name = cc.constraint_name "
                        + "where tc.table_schema = current_schema() and tc.table_name = :table",
                String.class)
                .setParameter("table", table)
                .getResultList());
    }
}
