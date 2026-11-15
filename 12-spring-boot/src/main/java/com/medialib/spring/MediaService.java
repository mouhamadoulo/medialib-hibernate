package com.medialib.spring;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Les transactions declaratives, et leurs trois pieges.
 *
 * <p>{@code @Transactional} n'est pas de la magie : c'est un proxy. Tout ce qui ne traverse
 * pas ce proxy n'est pas transactionnel, et c'est de la que viennent la plupart des surprises.
 */
@Service
public class MediaService {

    private final MediaRepository repository;

    public MediaService(MediaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Media cree(String titre, Integer annee) {
        return repository.save(new Media(titre, annee));
    }

    /**
     * {@code readOnly = true} : Hibernate passe la session en {@code FlushMode.MANUAL} et
     * saute le dirty checking. Une modification faite ici n'est donc jamais ecrite — sans
     * aucune erreur, sans aucun avertissement.
     */
    @Transactional(readOnly = true)
    public void renommeEnLectureSeule(Long id, String nouveauTitre) {
        repository.findById(id).ifPresent(media -> media.setTitle(nouveauTitre));
    }

    @Transactional
    public void renommeEnEcriture(Long id, String nouveauTitre) {
        repository.findById(id).ifPresent(media -> media.setTitle(nouveauTitre));
    }

    /**
     * LE piege de l'auto-invocation.
     *
     * <p>Cette methode n'est pas annotee. Elle appelle {@link #renommeEnEcriture} directement,
     * par {@code this} — donc <strong>sans passer par le proxy Spring</strong>. L'annotation
     * de la methode appelee n'a aucun effet : aucune transaction n'est ouverte.
     */
    public void renommeParAutoInvocation(Long id, String nouveauTitre) {
        renommeEnEcriture(id, nouveauTitre); // appel interne : le proxy est contourne
    }

    /** Dit si une transaction Spring est active a cet instant. */
    public boolean uneTransactionEstActive() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    @Transactional
    public boolean uneTransactionEstActiveDansUneMethodeAnnotee() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /** Appel interne vers une methode annotee : le proxy n'est pas traverse. */
    public boolean transactionVueParAutoInvocation() {
        return uneTransactionEstActiveDansUneMethodeAnnotee();
    }

    /**
     * {@code REQUIRES_NEW} : une transaction independante, qui survit a l'echec de celle qui
     * l'appelle. Utile pour un journal d'audit — on veut garder la trace meme si l'operation
     * echoue.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void creeDansUneTransactionIndependante(String titre, Integer annee) {
        repository.save(new Media(titre, annee));
    }

    @Transactional(readOnly = true)
    public Optional<Media> parTitre(String titre) {
        return repository.findByTitle(titre);
    }

    @Transactional(readOnly = true)
    public List<Media> tout() {
        return repository.findAll();
    }

    /**
     * La bonne pratique : le service charge ce dont la vue a besoin, dans sa transaction, et
     * rend des donnees deja plates. Le controleur n'a plus rien de paresseux a toucher — donc
     * plus besoin d'OSIV.
     */
    @Transactional(readOnly = true)
    public List<String> codesBarresDe(Long mediaId) {
        return repository.findById(mediaId)
                .map(media -> media.getCopies().stream().map(Copy::getBarcode).toList())
                .orElseGet(List::of);
    }
}
