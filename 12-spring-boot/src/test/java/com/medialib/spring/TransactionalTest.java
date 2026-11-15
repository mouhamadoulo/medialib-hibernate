package com.medialib.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@code @Transactional} : ce que la demarcation declarative apporte, et ses trois pieges.
 *
 * <p>Comparaison directe avec le chapitre 08, ou chaque transaction etait ouverte et validee
 * a la main. Le gain est reel. Le cout est qu'on ne voit plus ou commence et ou finit une
 * transaction, et qu'il faut connaitre le mecanisme — un proxy — pour ne pas se tromper.
 */
class TransactionalTest extends SpringTestBase {

    @Autowired
    private ServiceAvecRollback serviceAvecRollback;

    @BeforeEach
    void repartDeZero() {
        videLaBase();
    }

    @Test
    @DisplayName("readOnly = true : la modification n'est jamais ecrite, et rien ne le signale")
    void readonly_evite_le_dirty_checking() {
        Long id = service.cree("Une si longue lettre", 1979).getId();
        sql.reset();

        service.renommeEnLectureSeule(id, "Titre jamais ecrit");

        // Aucun UPDATE : Hibernate a mis la session en FlushMode.MANUAL.
        assertThat(sql.updateCount()).isZero();
        assertThat(service.parTitre("Une si longue lettre")).isPresent();
        assertThat(service.parTitre("Titre jamais ecrit")).isEmpty();
    }

    @Test
    @DisplayName("Sans readOnly, la meme methode ecrit bien")
    void sans_readonly_la_modification_est_ecrite() {
        Long id = service.cree("L'Aventure ambigue", 1961).getId();
        sql.reset();

        service.renommeEnEcriture(id, "Titre corrige");

        assertThat(sql.updateCount()).isEqualTo(1);
        assertThat(service.parTitre("Titre corrige")).isPresent();
    }

    @Test
    @DisplayName("LE piege : l'auto-invocation ne traverse pas le proxy")
    void l_auto_invocation_ne_traverse_pas_le_proxy() {
        // Une methode annotee, appelee depuis l'exterieur : transaction ouverte.
        assertThat(service.uneTransactionEstActiveDansUneMethodeAnnotee()).isTrue();

        // La MEME methode annotee, appelee depuis une autre methode du MEME objet :
        // l'appel passe par `this`, pas par le proxy Spring. L'annotation n'a aucun effet.
        assertThat(service.transactionVueParAutoInvocation()).isFalse();

        // Et une methode non annotee, evidemment.
        assertThat(service.uneTransactionEstActive()).isFalse();
    }

    @Test
    @DisplayName("Le piege en pratique : l'ecriture par auto-invocation ne persiste rien")
    void une_ecriture_par_auto_invocation_est_perdue() {
        Long id = service.cree("Les Bouts de bois de Dieu", 1960).getId();

        service.renommeParAutoInvocation(id, "Titre perdu");

        // Sans transaction, l'entite chargee n'est managee par personne au moment du retour.
        assertThat(service.parTitre("Titre perdu")).isEmpty();
        assertThat(service.parTitre("Les Bouts de bois de Dieu")).isPresent();
    }

    @Test
    @DisplayName("REQUIRES_NEW : la transaction imbriquee survit au rollback de l'appelante")
    void requires_new_ouvre_une_transaction_independante() {
        assertThatThrownBy(() -> serviceAvecRollback.ecritPuisEchoue())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("echec volontaire");

        // La transaction externe a ete annulee...
        assertThat(service.parTitre("Perdue au rollback")).isEmpty();
        // ...mais la transaction independante avait deja valide.
        assertThat(service.parTitre("Survit au rollback")).isPresent();
    }
}
