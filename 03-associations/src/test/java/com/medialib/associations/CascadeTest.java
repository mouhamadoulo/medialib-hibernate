package com.medialib.associations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les cascades propagent une operation du parent vers ses enfants. La question n'est jamais
 * « est-ce pratique » mais « cet enfant a-t-il une vie propre ».
 */
class CascadeTest extends AssociationsTestBase {

    @Test
    @DisplayName("CascadeType.PERSIST : un seul persist, deux insertions")
    void persist_en_cascade_propage_l_insertion() {
        inTransaction(session -> {
            Media media = new Media("Une si longue lettre");
            media.addCopy(new Copy("EX-0010"));
            sql().reset();

            session.persist(media); // on ne persiste QUE le parent

            session.flush();
            assertThat(sql().insertCount()).isEqualTo(2);
        });
    }

    @Test
    @DisplayName("CascadeType.REMOVE : supprimer l'oeuvre supprime ses exemplaires")
    void remove_en_cascade_supprime_les_enfants() {
        Long mediaId = inTransactionReturning(session -> {
            Media media = new Media("L'Aventure ambigue");
            media.addCopy(new Copy("EX-0011"));
            media.addCopy(new Copy("EX-0012"));
            session.persist(media);
            return media.getId();
        });

        inTransaction(session -> {
            Media media = session.find(Media.class, mediaId);
            media.getCopies().size(); // on force le chargement de la collection
            sql().reset();

            session.remove(media);

            session.flush();

            // Quatre DELETE, pas trois. Le quatrieme surprend : supprimer un Media vide
            // d'abord sa table de jonction @ManyToMany, meme lorsqu'elle ne contient rien —
            // Hibernate ne va pas verifier avant d'effacer.
            assertThat(sql().deleteCount()).isEqualTo(4);
            assertThat(sql().allNormalized())
                    .anyMatch(s -> s.startsWith("delete from media_author"));
            assertThat(sql().allNormalized())
                    .filteredOn(s -> s.startsWith("delete from copy"))
                    .hasSize(2);
        });

        Long restants = inTransactionReturning(session ->
                session.createQuery("select count(c) from Copy c", Long.class).getSingleResult());
        assertThat(restants).isZero();
    }

    @Test
    @DisplayName("orphanRemoval : retirer de la collection suffit a supprimer l'exemplaire")
    void retirer_de_la_collection_supprime_l_orphelin() {
        Long mediaId = inTransactionReturning(session -> {
            Media media = new Media("Les Bouts de bois de Dieu");
            media.addCopy(new Copy("EX-0013"));
            media.addCopy(new Copy("EX-0014"));
            session.persist(media);
            return media.getId();
        });

        inTransaction(session -> {
            Media media = session.find(Media.class, mediaId);
            Copy retire = media.getCopies().get(0);
            sql().reset();

            media.removeCopy(retire); // aucun session.remove() appele

            session.flush();
            assertThat(sql().deleteCount()).isEqualTo(1);
        });

        Long restants = inTransactionReturning(session ->
                session.createQuery("select count(c) from Copy c", Long.class).getSingleResult());
        assertThat(restants).isEqualTo(1L);
    }

    @Test
    @DisplayName("Sans orphanRemoval, retirer de la collection ne fait que detacher")
    void sans_orphan_removal_l_enfant_survit_a_son_retrait() {
        Long branchId = inTransactionReturning(session -> {
            Branch branch = new Branch("Annexe Nord");
            branch.getReservations().add(new Reservation(java.time.LocalDate.of(2026, 3, 1)));
            session.persist(branch);
            return branch.getId();
        });

        inTransaction(session -> {
            Branch branch = session.find(Branch.class, branchId);
            branch.getReservations().clear();
            sql().reset();
            session.flush();

            // La ligne de jonction disparait, la reservation reste : elle devient orpheline.
            assertThat(sql().deleteCount()).isEqualTo(1);
        });

        Long restantes = inTransactionReturning(session -> session
                .createQuery("select count(r) from Reservation r", Long.class).getSingleResult());
        assertThat(restantes).isEqualTo(1L); // toujours la, sans agence
    }
}
