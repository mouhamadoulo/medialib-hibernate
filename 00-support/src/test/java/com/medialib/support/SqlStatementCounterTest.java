package com.medialib.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le compteur SQL est le socle de tout le projet : chaque chapitre assertionne le nombre et
 * la forme des requetes emises. S'il compte mal, tous les tests du depot mentent.
 */
class SqlStatementCounterTest {

    @Test
    @DisplayName("Le DDL de hbm2ddl ne doit jamais etre compte comme du trafic applicatif")
    void ignore_le_ddl_pour_ne_compter_que_le_dml() {
        SqlStatementCounter counter = new SqlStatementCounter();

        counter.inspect("create table media (id bigint not null, primary key (id))");
        counter.inspect("alter table copy add constraint fk_media foreign key (media_id) references media");
        counter.inspect("drop table if exists media cascade");
        counter.inspect("truncate table media");
        counter.inspect("comment on table media is 'oeuvre du catalogue'");
        counter.inspect("select m.id from media m");

        assertThat(counter.count()).isEqualTo(1);
        assertThat(counter.selectCount()).isEqualTo(1);
        assertThat(counter.all()).containsExactly("select m.id from media m");
    }

    @Test
    @DisplayName("inspect respecte le contrat StatementInspector : il retourne le SQL inchange")
    void inspect_retourne_le_sql_inchange() {
        SqlStatementCounter counter = new SqlStatementCounter();
        String sql = "select m.id from media m";

        assertThat(counter.inspect(sql)).isEqualTo(sql);
    }

    @Test
    void classe_les_instructions_par_type() {
        SqlStatementCounter counter = new SqlStatementCounter();

        counter.inspect("select 1");
        counter.inspect("insert into media (id) values (1)");
        counter.inspect("update media set title = 'x'");
        counter.inspect("delete from media");

        assertThat(counter.selectCount()).isEqualTo(1);
        assertThat(counter.insertCount()).isEqualTo(1);
        assertThat(counter.updateCount()).isEqualTo(1);
        assertThat(counter.deleteCount()).isEqualTo(1);
        assertThat(counter.count()).isEqualTo(4);
        assertThat(counter.lastQuery()).startsWith("delete");
    }

    @Test
    @DisplayName("Le SQL emis par Hibernate arrive indente et sur plusieurs lignes")
    void classe_correctement_un_sql_multiligne_et_indente() {
        SqlStatementCounter counter = new SqlStatementCounter();

        counter.inspect("\n    select\n        m1_0.id \n    from\n        media m1_0");

        assertThat(counter.selectCount()).isEqualTo(1);
    }

    @Test
    void reset_vide_le_compteur() {
        SqlStatementCounter counter = new SqlStatementCounter();
        counter.inspect("select 1");

        counter.reset();

        assertThat(counter.count()).isZero();
        assertThat(counter.all()).isEmpty();
        assertThat(counter.lastQuery()).isNull();
    }

    @Test
    @DisplayName("Le test d'emprunt concurrent du module 08 inspecte depuis deux threads")
    void collecte_sans_perte_depuis_plusieurs_threads() throws Exception {
        SqlStatementCounter counter = new SqlStatementCounter();
        int threads = 8;
        int parThread = 200;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                for (int i = 0; i < parThread; i++) {
                    counter.inspect("select 1");
                }
            });
        }
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(counter.count()).isEqualTo(threads * parThread);
    }
}
