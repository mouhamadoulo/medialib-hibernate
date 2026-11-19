package com.medialib.advanced;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Appeler une procedure stockee depuis Hibernate.
 *
 * <p>Le cas d'usage n'est pas le calcul — HQL y suffirait — mais le code qui existe deja en
 * base et qu'on n'a pas le droit de reecrire : regle de gestion historique, traitement sous
 * droits particuliers, logique partagee avec un autre consommateur de la meme base.
 *
 * <p>La procedure est creee par le test lui-meme : {@code hbm2ddl.auto} ne genere que les
 * tables, et le chapitre 11 est le seul a demontrer Flyway. Un {@code create or replace} est
 * donc rejoue avant chaque methode.
 */
class ProcedureStockeeTest extends AvanceTestBase {

    @BeforeEach
    void creeLaProcedure() {
        inTransaction(session -> session.createNativeMutationQuery("""
                create or replace procedure compte_oeuvres(
                        in p_prefixe text, inout p_total integer)
                language plpgsql as $$
                begin
                    select count(*) into p_total from media where title like p_prefixe || '%';
                end;
                $$
                """).executeUpdate());
    }

    private void troisOeuvres() {
        inTransaction(session -> {
            session.persist(new Media("9782723604309", "Une si longue lettre"));
            session.persist(new Media("9782264030619", "Une autre histoire"));
            session.persist(new Media("9782266114781", "Les Bouts de bois de Dieu"));
        });
    }

    @Test
    @DisplayName("Procedure appelee par l'API : un `call`, et le resultat lu dans le parametre OUT")
    void la_procedure_est_appelee_par_un_call() {
        troisOeuvres();
        sql().reset();

        Integer total = inTransactionReturning(session -> {
            StoredProcedureQuery appel = session.createStoredProcedureQuery("compte_oeuvres")
                    .registerStoredProcedureParameter("p_prefixe", String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter("p_total", Integer.class, ParameterMode.INOUT);
            appel.setParameter("p_prefixe", "Une");

            // Un INOUT doit etre LIE avant l'appel, meme si sa valeur d'entree ne sert a rien :
            // sans cette ligne, Hibernate leve « The parameter named 'p_total' was not set ».
            // PostgreSQL n'a pas de parametre OUT pur dans une procedure — tout retour passe
            // par un INOUT, donc par une valeur d'entree a fournir.
            appel.setParameter("p_total", 0);

            appel.execute();
            return (Integer) appel.getOutputParameterValue("p_total");
        });

        // Deux titres commencent par « Une » : la procedure a bien recu son parametre d'entree.
        assertThat(total).isEqualTo(2);

        // Et le SQL emis n'est ni un select ni un update : c'est un appel. On le reconnait a
        // sa syntaxe d'echappement JDBC, que le pilote traduit pour PostgreSQL.
        assertThat(sql().allNormalized()).anyMatch(s -> s.contains("call compte_oeuvres"));
        assertThat(sql().selectCount()).isZero();
    }

    @Test
    @DisplayName("@NamedStoredProcedureQuery : le meme appel, declare sur l'entite")
    void la_procedure_peut_etre_declaree_sur_l_entite() {
        troisOeuvres();
        sql().reset();

        Integer total = inTransactionReturning(session -> {
            StoredProcedureQuery appel =
                    session.createNamedStoredProcedureQuery("Media.compteOeuvres");
            appel.setParameter("p_prefixe", "Les");
            appel.setParameter("p_total", 0);
            appel.execute();
            return (Integer) appel.getOutputParameterValue("p_total");
        });

        assertThat(total).isEqualTo(1);
        assertThat(sql().allNormalized()).anyMatch(s -> s.contains("call compte_oeuvres"));

        // L'interet de la forme declarative est le meme que pour @NamedQuery au chapitre 06 :
        // la signature est verifiee au demarrage et non au premier appel, et le nom de la
        // procedure ne traine plus dans le code appelant.
        //
        // Sa limite est celle de toute procedure stockee : elle est invisible du mapping, donc
        // du cache de second niveau et du contexte de persistance. Une procedure qui modifie
        // des lignes laisse Hibernate avec des entites peremees en main, sans qu'il en sache
        // rien — exactement comme un `update` en masse (voir le chapitre 11).
    }
}
