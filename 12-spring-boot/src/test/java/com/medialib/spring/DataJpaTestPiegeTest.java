package com.medialib.spring;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * {@code @DataJpaTest} : pratique, et trompeur.
 *
 * <p>Il rend chaque test transactionnel et l'annule a la fin. Deux consequences que beaucoup
 * de projets decouvrent tard :
 *
 * <ul>
 *   <li>Rien n'est jamais ecrit en base. Une contrainte violee au commit ne se declenche
 *       jamais, puisqu'il n'y a pas de commit.</li>
 *   <li>Le contexte de persistance reste ouvert pendant tout le test. Toute association
 *       paresseuse est accessible — donc aucun test de ce genre ne peut detecter le probleme
 *       que l'OSIV masque en production.</li>
 * </ul>
 *
 * <p>C'est pour cette raison que les autres tests de ce chapitre utilisent
 * {@code @SpringBootTest} sans transaction de test.
 */
@DataJpaTest
@Import(ConteneurPostgres.class)
class DataJpaTestPiegeTest {

    @Autowired
    private MediaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("save() n'ecrit rien : il attend un flush qui ne viendra jamais")
    void le_rollback_automatique_masque_les_erreurs_de_flush() {
        Media media = repository.save(new Media("Une si longue lettre", 1979));

        // L'identifiant est connu (la sequence a ete interrogee), mais rien n'est en base.
        assertThat(media.getId()).isNotNull();

        Number enBase = (Number) entityManager
                .createNativeQuery("select count(*) from media")
                .getSingleResult();
        assertThat(enBase.longValue()).isZero();

        // Il faut le demander explicitement pour voir ce qu'une vraie application verrait.
        entityManager.flush();

        Number apresFlush = (Number) entityManager
                .createNativeQuery("select count(*) from media")
                .getSingleResult();
        assertThat(apresFlush.longValue()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Le contexte reste ouvert : aucune LazyInitializationException n'est possible")
    void le_contexte_ouvert_rend_les_lazy_toujours_accessibles() {
        Media media = new Media("L'Aventure ambigue", 1961);
        media.addCopy(new Copy("EX-0001"));
        repository.save(media);
        entityManager.flush();
        entityManager.clear();

        Media relu = repository.findById(media.getId()).orElseThrow();

        // Hors transaction, cette ligne echouerait. Ici, jamais — et c'est le piege : un test
        // vert ne dit rien de ce qui se passera derriere un controleur.
        assertThat(relu.getCopies()).hasSize(1);
    }
}
