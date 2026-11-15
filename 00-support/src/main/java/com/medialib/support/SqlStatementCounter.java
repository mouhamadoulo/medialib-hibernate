package com.medialib.support;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Compte et conserve le SQL reellement emis par Hibernate.
 *
 * <p>C'est l'instrument central du projet : au lieu d'assertionner qu'une methode « marche »,
 * chaque test de ce depot assertionne <em>combien</em> de requetes elle a declenchees et
 * <em>de quelle forme</em> elles sont. C'est la seule facon de voir un N+1, un verrou
 * pessimiste ou un batch JDBC.
 *
 * <p>Branche sur Hibernate via le reglage {@code hibernate.session_factory.statement_inspector},
 * donc independant de la facon dont le module obtient ses connexions. Un proxy de
 * {@code DataSource} imposerait au contraire sa propre configuration aux modules 01, 10 et 11,
 * dont la lecon est precisement la facon dont la connexion est acquise.
 *
 * <p><strong>Le DDL n'est jamais compte.</strong> Le schema est genere par
 * {@code hbm2ddl.auto=create-drop} et le nettoyage entre tests passe par {@code truncate} :
 * sans ce filtre, des dizaines de {@code create table} fausseraient chaque assertion.
 *
 * <p>La collecte est thread-safe : le test d'emprunt concurrent du module 08 inspecte depuis
 * deux threads simultanement.
 */
public class SqlStatementCounter implements StatementInspector {

    private static final List<String> DDL_PREFIXES =
            List.of("create", "alter", "drop", "truncate", "comment", "set ", "call current_schema");

    private final List<String> statements = new CopyOnWriteArrayList<>();

    @Override
    public String inspect(String sql) {
        if (sql != null && !estDuDdl(sql)) {
            statements.add(sql);
        }
        return sql; // contrat StatementInspector : on observe, on ne reecrit pas
    }

    public void reset() {
        statements.clear();
    }

    /** Le SQL applicatif emis depuis le dernier {@link #reset()}, dans l'ordre. */
    public List<String> all() {
        return List.copyOf(statements);
    }

    public int count() {
        return statements.size();
    }

    public int selectCount() {
        return compteCommencantPar("select");
    }

    public int insertCount() {
        return compteCommencantPar("insert");
    }

    public int updateCount() {
        return compteCommencantPar("update");
    }

    public int deleteCount() {
        return compteCommencantPar("delete");
    }

    /** La derniere requete emise, normalisee, ou {@code null} si le compteur est vide. */
    public String lastQuery() {
        return statements.isEmpty() ? null : normalise(statements.get(statements.size() - 1));
    }

    /** Toutes les requetes emises, normalisees, pour assertionner leur forme. */
    public List<String> allNormalized() {
        return statements.stream().map(SqlStatementCounter::normalise).toList();
    }

    private int compteCommencantPar(String motCle) {
        return (int) statements.stream().map(SqlStatementCounter::normalise).filter(s -> s.startsWith(motCle)).count();
    }

    private static boolean estDuDdl(String sql) {
        String normalise = normalise(sql);
        return DDL_PREFIXES.stream().anyMatch(normalise::startsWith);
    }

    /**
     * Hibernate emet son SQL indente et sur plusieurs lignes. On replie en une seule ligne
     * minuscule pour que les assertions sur la forme ({@code contains("for update")}) soient
     * lisibles et stables.
     */
    private static String normalise(String sql) {
        return sql.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
