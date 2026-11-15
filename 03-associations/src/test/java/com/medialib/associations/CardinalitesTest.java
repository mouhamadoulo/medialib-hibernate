package com.medialib.associations;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les quatre cardinalites, et surtout la question qui decide de tout : <strong>qui ecrit la
 * cle etrangere ?</strong>
 */
class CardinalitesTest extends AssociationsTestBase {

    @Test
    @DisplayName("@ManyToOne : le cote proprietaire porte la cle etrangere, dans sa propre table")
    void copy_vers_media_ecrit_la_cle_etrangere() {
        inTransaction(session -> {
            Media media = new Media("Une si longue lettre");
            Copy copy = new Copy("EX-0001");
            copy.setMedia(media);

            sql().reset();
            session.persist(media);
            session.persist(copy);
            session.flush();

            // Deux INSERT, pas trois : la cle etrangere voyage dans l'insertion de l'exemplaire.
            assertThat(sql().insertCount()).isEqualTo(2);
            assertThat(sql().allNormalized())
                    .anyMatch(s -> s.startsWith("insert into copy") && s.contains("media_id"));
        });
    }

    @Test
    @DisplayName("@OneToMany unidirectionnel : Hibernate n'a nulle part ou ecrire, il fabrique une table de jonction")
    void unidirectionnel_impose_une_table_de_jonction() {
        inTransaction(session -> {
            Branch branch = new Branch("Mediatheque centrale");
            branch.getReservations().add(new Reservation(LocalDate.of(2026, 3, 1)));

            sql().reset();
            session.persist(branch);
            session.flush();

            // 3 INSERT pour 2 entites : agence, reservation, PUIS la ligne de jonction.
            assertThat(sql().insertCount()).isEqualTo(3);
            assertThat(sql().allNormalized())
                    .anyMatch(s -> s.startsWith("insert into branch_reservation"));
        });
    }

    @Test
    @DisplayName("@OneToMany bidirectionnel : une table de moins, une insertion de moins")
    void bidirectionnel_avec_mappedby_n_ecrit_qu_une_fois() {
        inTransaction(session -> {
            Media media = new Media("L'Aventure ambigue");
            media.addCopy(new Copy("EX-0002"));

            sql().reset();
            session.persist(media);
            session.flush();

            // 2 INSERT seulement, et aucune table de jonction.
            assertThat(sql().insertCount()).isEqualTo(2);
            assertThat(sql().allNormalized()).noneMatch(s -> s.contains("media_copies"));
        });
    }

    @Test
    @DisplayName("Le piege du bidirectionnel : ajouter a la collection ne suffit pas")
    void oublier_de_synchroniser_les_deux_cotes_laisse_la_fk_nulle() {
        Long copyId = inTransactionReturning(session -> {
            Media media = new Media("Les Bouts de bois de Dieu");
            Copy copy = new Copy("EX-0003");

            // On ajoute du cote inverse SEULEMENT — l'erreur classique.
            media.getCopies().add(copy);

            session.persist(media);
            session.persist(copy);
            return copy.getId();
        });

        Media mediaDeLExemplaire =
                inTransactionReturning(session -> session.find(Copy.class, copyId).getMedia());

        // La collection avait l'air juste en memoire, mais rien n'a ete ecrit en base.
        assertThat(mediaDeLExemplaire).isNull();
    }

    @Test
    @DisplayName("@OneToOne avec @MapsId : une seule colonne, cle primaire et etrangere a la fois")
    void mapsid_partage_la_cle_primaire() {
        Long memberId = inTransactionReturning(session -> {
            Member member = new Member("ADH-001", "Awa Diop");
            member.setCard(new MembershipCard(LocalDate.of(2026, 1, 15)));
            session.persist(member);
            return member.getId();
        });

        inTransaction(session -> {
            MembershipCard card = session.find(MembershipCard.class, memberId);

            assertThat(card).isNotNull();
            assertThat(card.getId()).isEqualTo(memberId); // meme valeur que l'adherent
            assertThat(card.getMember().getMembershipNumber()).isEqualTo("ADH-001");
        });

        // Aucune colonne member_id distincte : la cle primaire tient les deux roles.
        Long colonnes = inTransactionReturning(session -> session.createNativeQuery(
                "select count(*) from information_schema.columns "
                        + "where table_name = 'membership_card'", Long.class)
                .getSingleResult());
        assertThat(colonnes).isEqualTo(2L); // id et issued_on
    }

    @Test
    @DisplayName("@ManyToMany : une table de jonction, geree par Hibernate")
    void media_et_author_passent_par_une_table_de_jonction() {
        inTransaction(session -> {
            Media media = new Media("Une si longue lettre");
            media.addAuthor(new Author("Mariama Ba"));

            sql().reset();
            session.persist(media);
            session.flush();

            assertThat(sql().insertCount()).isEqualTo(3); // media, author, media_author
            assertThat(sql().allNormalized())
                    .anyMatch(s -> s.startsWith("insert into media_author"));
        });
    }

    @Test
    @DisplayName("Un attribut sur la relation : le @ManyToMany ne suffit plus")
    void un_attribut_sur_la_relation_impose_une_entite_d_association() {
        Long loanId = inTransactionReturning(session -> {
            Member member = new Member("ADH-002", "Ibrahima Fall");
            Media media = new Media("L'Aventure ambigue");
            Copy copy = new Copy("EX-0004");
            media.addCopy(copy);
            session.persist(member);
            session.persist(media);

            Loan loan = new Loan(member, copy, LocalDate.of(2026, 3, 1));
            session.persist(loan);
            return loan.getId();
        });

        inTransaction(session -> {
            Loan loan = session.find(Loan.class, loanId);

            // La date d'emprunt n'aurait eu nulle part ou vivre dans une table de jonction.
            assertThat(loan.getLoanedOn()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(loan.getMember().getMembershipNumber()).isEqualTo("ADH-002");
            assertThat(loan.getCopy().getBarcode()).isEqualTo("EX-0004");
        });
    }
}
