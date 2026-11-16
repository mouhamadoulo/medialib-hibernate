package com.medialib.spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le compteur local de ce module est une copie de celui de {@code 00-support}.
 *
 * <p>Une copie non testee est une copie qui divergera. Ce test porte la meme verification que
 * {@code SqlStatementCounterTest.classe_correctement_un_sql_multiligne_et_indente} : Hibernate
 * emet son SQL indente et sur plusieurs lignes, et un compteur qui ne replie pas les sauts de
 * ligne fait echouer toute assertion dont la cible enjambe un retour a la ligne.
 */
class CompteurSqlTest {

    @Test
    @DisplayName("Le SQL multiligne d'Hibernate est replie sur une seule ligne")
    void replie_le_sql_multiligne_sur_une_ligne() {
        CompteurSql compteur = new CompteurSql();

        compteur.inspect("""
                select
                    m1_0.id,
                    m1_0.title
                from
                    media m1_0
                order by
                    m1_0.title""");

        assertThat(compteur.selectCount()).isEqualTo(1);
        // L'assertion qui compte : une cible qui enjambe deux lignes du SQL d'origine.
        assertThat(compteur.lastQuery()).contains("from media m1_0");
        assertThat(compteur.lastQuery()).doesNotContain("\n");
    }

    @Test
    @DisplayName("Le DDL n'est pas compte, comme dans les onze autres chapitres")
    void ignore_le_ddl() {
        CompteurSql compteur = new CompteurSql();

        compteur.inspect("create table media (id bigint not null, primary key (id))");
        compteur.inspect("alter table copy add constraint fk_media foreign key (media_id) references media");
        compteur.inspect("select m.id from media m");

        assertThat(compteur.count()).isEqualTo(1);
        assertThat(compteur.selectCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("inspect retourne le SQL inchange : contrat StatementInspector")
    void inspect_retourne_le_sql_inchange() {
        CompteurSql compteur = new CompteurSql();
        String sql = "select m.id from media m";

        assertThat(compteur.inspect(sql)).isEqualTo(sql);
    }

    @Test
    void classe_les_instructions_par_type() {
        CompteurSql compteur = new CompteurSql();

        compteur.inspect("select 1");
        compteur.inspect("insert into media (id) values (1)");
        compteur.inspect("update media set title = 'x'");

        assertThat(compteur.selectCount()).isEqualTo(1);
        assertThat(compteur.insertCount()).isEqualTo(1);
        assertThat(compteur.updateCount()).isEqualTo(1);
        assertThat(compteur.count()).isEqualTo(3);
    }
}
