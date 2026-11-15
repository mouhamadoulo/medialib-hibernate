package com.medialib.fetching;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityGraph;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le probleme N+1, reproduit, chiffre, puis resolu de quatre facons.
 *
 * <p>C'est le chapitre le plus important du depot. Le N+1 ne casse rien : le code marche, les
 * tests passent, les donnees sont justes. Il rend simplement l'application cent fois plus
 * lente, et seul le SQL emis le revele.
 */
class NPlusUnTest extends ChargementTestBase {

    @Test
    @DisplayName("Le N+1 : 1 requete pour la liste, puis 1 par element. 101 au total.")
    void le_n_plus_un_est_reproduit_et_chiffre() {
        centOeuvresEmpruntees();

        inTransaction(session -> {
            sql().reset();

            List<Loan> emprunts =
                    session.createQuery("select l from Loan l", Loan.class).getResultList();

            assertThat(sql().selectCount()).isEqualTo(1); // la liste : une requete

            // Et maintenant, la boucle d'affichage la plus anodine du monde.
            emprunts.forEach(l -> l.getMember().getFullName());

            assertThat(sql().selectCount()).isEqualTo(1 + NOMBRE_D_OEUVRES); // 101
        });
    }

    @Test
    @DisplayName("join fetch : tout en une seule requete")
    void join_fetch_ramene_tout_en_une_requete() {
        centOeuvresEmpruntees();

        inTransaction(session -> {
            sql().reset();

            List<Loan> emprunts = session.createQuery(
                    "select l from Loan l join fetch l.member", Loan.class).getResultList();

            emprunts.forEach(l -> l.getMember().getFullName());

            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("join");
            assertThat(emprunts).hasSize(NOMBRE_D_OEUVRES);
        });
    }

    @Test
    @DisplayName("@EntityGraph nomme : meme resultat, sans toucher a la requete")
    void entitygraph_nomme_resout_aussi() {
        centOeuvresEmpruntees();

        inTransaction(session -> {
            EntityGraph<?> graphe = session.getEntityGraph("Loan.avecAdherentEtOeuvre");
            sql().reset();

            List<Loan> emprunts = session.createQuery("select l from Loan l", Loan.class)
                    .setHint("jakarta.persistence.fetchgraph", graphe)
                    .getResultList();

            emprunts.forEach(l -> l.getMember().getFullName());

            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("join");
        });
    }

    @Test
    @DisplayName("@BatchSize(10) sur Media : 11 requetes au lieu de 101, sans changer le code appelant")
    void batchsize_ramene_a_un_nombre_borne_de_requetes() {
        centOeuvresEmpruntees();

        inTransaction(session -> {
            List<Copy> exemplaires =
                    session.createQuery("select c from Copy c", Copy.class).getResultList();
            sql().reset();

            // Exactement la meme boucle que le test du N+1, sur une entite qui, elle,
            // porte @BatchSize(size = 10).
            exemplaires.forEach(c -> c.getMedia().getTitle());

            assertThat(sql().selectCount()).isEqualTo(NOMBRE_D_OEUVRES / 10); // 10 paquets de 10
            assertThat(sql().lastQuery()).contains("= any (?)");
        });
    }

    @Test
    @DisplayName("La projection DTO : une requete, et seulement les colonnes affichees")
    void une_projection_dto_est_souvent_la_vraie_reponse() {
        centOeuvresEmpruntees();

        inTransaction(session -> {
            sql().reset();

            List<TitreEtAdherent> lignes = session.createQuery(
                    "select new com.medialib.fetching.TitreEtAdherent(m.title, mb.fullName) "
                            + "from Loan l join l.member mb join l.copy c join c.media m",
                    TitreEtAdherent.class)
                    .getResultList();

            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(lignes).hasSize(NOMBRE_D_OEUVRES);

            // Rien d'autre que les deux colonnes affichees : ni identifiants, ni code-barres,
            // ni agence. Et aucune entite managee, donc aucun dirty checking a la cle.
            assertThat(sql().lastQuery()).doesNotContain("barcode", "branch_id");
        });
    }

    @Test
    @DisplayName("Hibernate.initialize : charger explicitement, dans la session")
    void hibernate_initialize_charge_a_la_demande() {
        centOeuvresEmpruntees();

        inTransaction(session -> {
            Loan emprunt = session.createQuery("select l from Loan l", Loan.class)
                    .setMaxResults(1).getSingleResult();
            sql().reset();

            assertThat(org.hibernate.Hibernate.isInitialized(emprunt.getMember())).isFalse();

            org.hibernate.Hibernate.initialize(emprunt.getMember());

            assertThat(sql().selectCount()).isEqualTo(1);
            assertThat(org.hibernate.Hibernate.isInitialized(emprunt.getMember())).isTrue();
        });
    }
}
