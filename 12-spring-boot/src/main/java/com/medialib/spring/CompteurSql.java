package com.medialib.spring;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Le meme compteur que dans les onze autres chapitres, en version locale.
 *
 * <p>Ce module n'utilise pas {@code 00-support} (voir le pom), il porte donc sa propre copie.
 * Le principe est identique : un {@code StatementInspector} d'Hibernate, branche ici par
 * {@link ConfigurationHibernate} au lieu d'un reglage passe a la main.
 *
 * <p>Une copie non testee est une copie qui divergera : {@code CompteurSqlTest} porte donc les
 * memes verifications que son jumeau. Elle a d'ailleurs deja servi — cette classe contenait
 * {@code replaceAll("\s+", " ")} ecrit avec un seul antislash, ce qui en Java n'est pas la
 * regex {@code \s+} mais l'echappement d'espace (JLS 3.10.7), donc la chaine {@code " +"}.
 * Le compteur ne repliait alors que les suites d'espaces, jamais les sauts de ligne.
 */
public class CompteurSql implements StatementInspector {

    private final List<String> instructions = new CopyOnWriteArrayList<>();

    @Override
    public String inspect(String sql) {
        if (sql != null && !estDuDdl(sql)) {
            instructions.add(normalise(sql));
        }
        return sql;
    }

    public void reset() {
        instructions.clear();
    }

    public List<String> all() {
        return List.copyOf(instructions);
    }

    public int count() {
        return instructions.size();
    }

    public int selectCount() {
        return (int) instructions.stream().filter(s -> s.startsWith("select")).count();
    }

    public int updateCount() {
        return (int) instructions.stream().filter(s -> s.startsWith("update")).count();
    }

    public int insertCount() {
        return (int) instructions.stream().filter(s -> s.startsWith("insert")).count();
    }

    public String lastQuery() {
        return instructions.isEmpty() ? null : instructions.get(instructions.size() - 1);
    }

    private static boolean estDuDdl(String sql) {
        String n = normalise(sql);
        return n.startsWith("create") || n.startsWith("drop") || n.startsWith("alter")
                || n.startsWith("truncate") || n.startsWith("comment")
                || n.startsWith("set ") || n.startsWith("call current_schema");
    }

    private static String normalise(String sql) {
        return sql.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
