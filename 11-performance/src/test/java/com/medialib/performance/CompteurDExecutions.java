package com.medialib.performance;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;

/**
 * Compte les <strong>allers-retours JDBC</strong>, et non les requetes.
 *
 * <p>C'est la seule raison pour laquelle ce chapitre utilise {@code datasource-proxy} au lieu
 * du {@code SqlStatementCounter} des dix autres. Le {@code StatementInspector} d'Hibernate
 * voit passer le texte d'une requete au moment ou elle est preparee : il ne peut pas distinguer
 * cent INSERT envoyes un par un de cent INSERT regroupes en cinq lots. Or c'est exactement la
 * difference que le batching fait, et c'est elle qu'on vient mesurer.
 *
 * <p>{@code afterQuery} est appele une fois par appel a {@code execute} ou
 * {@code executeBatch} : une execution = un aller-retour reseau.
 */
public class CompteurDExecutions implements QueryExecutionListener {

    /** Une execution JDBC observee : son SQL normalise, et combien d'instructions elle portait. */
    private record Execution(String sql, int instructions) {
    }

    private final List<Execution> executions = new CopyOnWriteArrayList<>();

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        // rien a faire avant
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        for (QueryInfo info : queryInfoList) {
            String sql = normalise(info.getQuery());
            if (estDuDdl(sql)) {
                continue;
            }
            executions.add(new Execution(sql, Math.max(1, info.getParametersList().size())));
        }
    }

    public void reset() {
        executions.clear();
    }

    /** Nombre total d'allers-retours JDBC, toutes instructions confondues. */
    public int executions() {
        return executions.size();
    }

    /** Nombre total d'instructions SQL logiques. Le batching ne le change pas. */
    public int requetes() {
        return executions.stream().mapToInt(Execution::instructions).sum();
    }

    /**
     * Allers-retours pour les instructions commencant par ce mot-cle.
     *
     * <p>Indispensable pour isoler les INSERT des appels a la sequence, qui sont des SELECT et
     * fausseraient tout comptage global.
     */
    public int executionsDe(String motCle) {
        return (int) executions.stream().filter(e -> e.sql().startsWith(motCle)).count();
    }

    /** Instructions logiques commencant par ce mot-cle. */
    public int requetesDe(String motCle) {
        return executions.stream()
                .filter(e -> e.sql().startsWith(motCle))
                .mapToInt(Execution::instructions)
                .sum();
    }

    private static boolean estDuDdl(String sql) {
        return sql.startsWith("create") || sql.startsWith("drop") || sql.startsWith("alter")
                || sql.startsWith("truncate") || sql.startsWith("set ");
    }

    private static String normalise(String sql) {
        return sql.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
