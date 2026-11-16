package com.medialib.spring;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
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
    @DisplayName("save() n'ecrit rien tout de suite : il attend un flush")
    void le_rollback_automatique_masque_les_erreurs_de_flush() {
        Media media = repository.save(new Media("Une si longue lettre", 1979));

        // L'identifiant est connu (la sequence a ete interrogee), mais rien n'est encore ecrit.
        assertThat(media.getId()).isNotNull();

        // Pour l'observer, il faut empecher l'auto-flush — voir le test suivant.
        entityManager.setFlushMode(FlushModeType.COMMIT);
        assertThat(nombreDeMedias()).isZero();

        // Et une fois le flush demande, la ligne est la... dans CETTE transaction seulement.
        entityManager.setFlushMode(FlushModeType.AUTO);
        entityManager.flush();
        assertThat(nombreDeMedias()).isEqualTo(1L);

        // A la fin du test, @DataJpaTest annule tout. Aucune contrainte differee, aucun
        // declencheur, aucun comportement de commit ne sera jamais exerce par ce test.
    }

    @Test
    @DisplayName("Une requete native declenche un auto-flush de TOUTE la session")
    void une_requete_native_force_un_auto_flush() {
        repository.save(new Media("L'Aventure ambigue", 1961));

        // On n'a pas appele flush(). Et pourtant la requete native voit deja la ligne :
        // faute de savoir quelles tables une requete native touche, Hibernate vide par
        // prudence tout ce qui est en attente avant de l'executer.
        //
        // Meme logique qu'au chapitre 09, ou une ecriture native invalidait toutes les
        // regions du cache L2. Le prix de l'ignorance est toujours le pessimisme.
        assertThat(nombreDeMedias()).isEqualTo(1L);
    }

    private long nombreDeMedias() {
        return ((Number) entityManager
                .createNativeQuery("select count(*) from media")
                .getSingleResult()).longValue();
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
