package com.medialib.advanced;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Restreindre ce qu'on voit : {@code @SQLRestriction} (toujours), {@code @Filter} (a la
 * demande), et le soft delete qui s'appuie sur le premier.
 */
class SoftDeleteEtFiltresTest extends AvanceTestBase {

    @Test
    @DisplayName("remove emet un UPDATE, pas un DELETE")
    void remove_emet_un_update_pas_un_delete() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("9782723604309", "Une si longue lettre");
            session.persist(media);
            return media.getId();
        });

        inTransaction(session -> {
            Media media = session.find(Media.class, id);
            sql().reset();

            session.remove(media);
            session.flush();

            assertThat(sql().deleteCount()).isZero();
            assertThat(sql().updateCount()).isEqualTo(1);
            assertThat(sql().lastQuery()).contains("set archived");
        });

        // La ligne est toujours en base : les emprunts passes peuvent la designer.
        Long enBase = inTransactionReturning(session -> session
                .createNativeQuery("select count(*) from media where id = :id", Long.class)
                .setParameter("id", id).getSingleResult());
        assertThat(enBase).isEqualTo(1L);
    }

    @Test
    @DisplayName("@SQLRestriction masque les lignes archivees a toutes les requetes")
    void sqlrestriction_masque_les_lignes_archivees() {
        Long id = inTransactionReturning(session -> {
            Media media = new Media("9782264030619", "L'Aventure ambigue");
            session.persist(media);
            return media.getId();
        });

        inTransaction(session -> session.remove(session.find(Media.class, id)));

        inTransaction(session -> {
            sql().reset();

            // find ne la trouve plus...
            assertThat(session.find(Media.class, id)).isNull();
            // ...et une requete non plus.
            List<Media> catalogue =
                    session.createQuery("select m from Media m", Media.class).getResultList();
            assertThat(catalogue).isEmpty();

            assertThat(sql().allNormalized()).allMatch(s -> s.contains("archived"));
        });
    }

    @Test
    @DisplayName("Un @Filter parametre restreint a une agence, sans toucher aux requetes")
    void un_filter_parametre_restreint_a_une_agence() {
        inTransaction(session -> {
            session.persist(new Copy("EX-D-1", "DAKAR"));
            session.persist(new Copy("EX-D-2", "DAKAR"));
            session.persist(new Copy("EX-SL-1", "SAINT-LOUIS"));
        });

        inTransaction(session -> {
            session.enableFilter("parAgence").setParameter("agence", "DAKAR");
            sql().reset();

            // Exactement la meme requete que sans filtre.
            List<Copy> visibles =
                    session.createQuery("select c from Copy c", Copy.class).getResultList();

            assertThat(visibles).hasSize(2);
            assertThat(visibles).allMatch(c -> c.getBranchCode().equals("DAKAR"));
            assertThat(sql().lastQuery()).contains("branch_code");
        });
    }

    @Test
    @DisplayName("Filtre desactive : la meme requete voit tout")
    void le_filter_desactive_ne_restreint_rien() {
        inTransaction(session -> {
            session.persist(new Copy("EX-D-1", "DAKAR"));
            session.persist(new Copy("EX-SL-1", "SAINT-LOUIS"));
        });

        inTransaction(session -> {
            sql().reset();
            List<Copy> toutes =
                    session.createQuery("select c from Copy c", Copy.class).getResultList();

            assertThat(toutes).hasSize(2);
            assertThat(sql().lastQuery()).doesNotContain("branch_code=");
        });
    }

    @Test
    @DisplayName("Le filtre vit sur la session : deux sessions, deux visibilites")
    void le_filter_est_porte_par_la_session() {
        inTransaction(session -> {
            session.persist(new Copy("EX-D-1", "DAKAR"));
            session.persist(new Copy("EX-SL-1", "SAINT-LOUIS"));
        });

        try (Session dakar = sessionFactory().openSession();
                Session saintLouis = sessionFactory().openSession()) {

            dakar.enableFilter("parAgence").setParameter("agence", "DAKAR");
            saintLouis.enableFilter("parAgence").setParameter("agence", "SAINT-LOUIS");

            assertThat(dakar.createQuery("select c from Copy c", Copy.class).getResultList())
                    .extracting(Copy::getBarcode).containsExactly("EX-D-1");
            assertThat(saintLouis.createQuery("select c from Copy c", Copy.class).getResultList())
                    .extracting(Copy::getBarcode).containsExactly("EX-SL-1");
        }
    }
}
