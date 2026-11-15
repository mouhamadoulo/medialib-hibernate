package com.medialib.spring;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Demontre qu'une transaction REQUIRES_NEW survit au rollback de celle qui l'appelle. */
@Service
public class ServiceAvecRollback {

    private final MediaService mediaService;
    private final MediaRepository repository;

    public ServiceAvecRollback(MediaService mediaService, MediaRepository repository) {
        this.mediaService = mediaService;
        this.repository = repository;
    }

    /**
     * Ecrit dans la transaction courante, puis dans une transaction independante, puis echoue.
     *
     * <p>Au retour : la premiere ecriture a disparu, la seconde est restee.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void ecritPuisEchoue() {
        repository.save(new Media("Perdue au rollback", 2026));
        mediaService.creeDansUneTransactionIndependante("Survit au rollback", 2026);
        throw new IllegalStateException("echec volontaire");
    }
}
