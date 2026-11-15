package com.medialib.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Deux mappings qui ressemblent a des details de confort et n'en sont pas :
 * {@code @Enumerated} decide de la resistance des donnees au temps, {@code @Formula} decide de
 * ce qui est calcule par la base plutot que par le code.
 */
class EnumEtFormulaTest extends MappingTestBase {

    private Long unVinyle() {
        return inTransactionReturning(session -> {
            Media media = new Media("Pirates Choice", new Isbn("978-1-234-56789-7"),
                    MediaFormat.VINYL, null);
            session.persist(media);
            return media.getId();
        });
    }

    @Test
    @DisplayName("ORDINAL stocke une position : la base ignore le nom de la constante")
    void ordinal_stocke_la_position_de_la_constante() {
        Long id = unVinyle();

        Object brut = inTransactionReturning(session ->
                session.createNativeQuery("select format_ordinal from media where id = :id", Object.class)
                        .setParameter("id", id)
                        .getSingleResult());

        // VINYL est la troisieme constante : position 2.
        assertThat(((Number) brut).intValue()).isEqualTo(2);
    }

    @Test
    @DisplayName("Le piege ORDINAL : reordonner l'enumeration reinterprete les lignes existantes")
    void ordinal_casse_au_reordonnancement_de_l_enum() {
        Long id = unVinyle();

        // On simule l'insertion d'une constante en tete de l'enumeration : toutes les positions
        // deja en base glissent d'un cran. Ici, le vinyle (2) devient un DVD (1).
        inTransaction(session -> session
                .createNativeMutationQuery("update media set format_ordinal = format_ordinal - 1 where id = :id")
                .setParameter("id", id)
                .executeUpdate());

        Media relu = inTransactionReturning(session -> session.find(Media.class, id));

        assertThat(relu.getFormatOrdinal()).isEqualTo(MediaFormat.DVD); // ce n'est plus un vinyle
        assertThat(relu.getFormatString()).isEqualTo(MediaFormat.VINYL); // la colonne STRING, elle, a tenu
    }

    @Test
    @DisplayName("STRING stocke le nom : insensible a l'ordre de declaration")
    void string_resiste_au_reordonnancement() {
        Long id = unVinyle();

        String brut = inTransactionReturning(session ->
                session.createNativeQuery("select format_string from media where id = :id", String.class)
                        .setParameter("id", id)
                        .getSingleResult());

        assertThat(brut).isEqualTo("VINYL");
    }

    @Test
    @DisplayName("@Formula est calculee dans le SELECT : jamais de seconde requete")
    void un_champ_formula_est_calcule_en_sql() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("Une si longue lettre", new Isbn("978-2-7236-0430-9"),
                    MediaFormat.BOOK, 165);
            session.persist(media);
            return media.getId();
        });

        inTransaction(session -> {
            sql().reset();

            Media media = session.find(Media.class, id);

            assertThat(sql().selectCount()).isEqualTo(1);
            // La formule est injectee dans le SELECT, avec l'alias de la table resolu :
            // "length(title)" devient "length(m1_0.title)".
            assertThat(sql().lastQuery()).containsPattern("length\\(\\w+\\.title\\)");
            assertThat(media.getTitleLength()).isEqualTo("Une si longue lettre".length());
            assertThat(sql().selectCount()).isEqualTo(1); // toujours une seule requete
        });
    }

    @Test
    @DisplayName("@Transient n'atteint jamais la base")
    void un_champ_transient_n_est_pas_persiste() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("L'Aventure ambigue", new Isbn("978-2-264-03061-9"),
                    MediaFormat.BOOK, 191);
            media.setResume("Un roman de Cheikh Hamidou Kane");
            session.persist(media);
            return media.getId();
        });

        inTransaction(session -> {
            sql().reset();
            Media relu = session.find(Media.class, id);

            assertThat(relu.getResume()).isNull();
            assertThat(sql().lastQuery()).doesNotContain("resume");
        });
    }
}
