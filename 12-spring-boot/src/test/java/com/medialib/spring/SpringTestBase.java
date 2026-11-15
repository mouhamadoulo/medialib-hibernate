package com.medialib.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Socle des tests du chapitre.
 *
 * <p>Deliberement <strong>sans</strong> {@link Transactional} sur la classe : c'est le piege
 * que {@code DataJpaTestPiegeTest} documente. Un test transactionnel masque le moment du flush
 * et annule tout a la fin — ce qui est exactement ce qu'on ne veut pas quand le sujet est le
 * comportement transactionnel lui-meme.
 */
@SpringBootTest
@Import(ConteneurPostgres.class)
abstract class SpringTestBase {

    @Autowired
    protected MediaRepository repository;

    @Autowired
    protected MediaService service;

    @Autowired
    protected CompteurSql sql;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    /** Table rase entre deux tests : aucun test n'herite de l'etat d'un autre. */
    protected void videLaBase() {
        transactionTemplate.executeWithoutResult(status -> repository.deleteAllInBatch());
        sql.reset();
    }
}
