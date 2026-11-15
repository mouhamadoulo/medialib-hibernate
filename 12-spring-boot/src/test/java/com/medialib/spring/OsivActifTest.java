package com.medialib.spring;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * OSIV <strong>actif</strong> — c'est le defaut de Spring Boot.
 *
 * <p>Un filtre maintient la session Hibernate ouverte jusqu'a la fin du rendu HTTP. Le
 * controleur fautif fonctionne donc : il peut toucher une collection paresseuse hors
 * transaction.
 *
 * <p>Ce que ca coute, et qu'aucun test ne peut rendre spectaculaire : une connexion de base
 * est retenue pendant toute la duree de la requete HTTP, rendu compris, et les requetes SQL
 * partent depuis la couche de presentation — donc hors de tout controle du service, et
 * invisibles a la lecture du code metier. Sur une application chargee, c'est le pool de
 * connexions qui devient le goulot.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ConteneurPostgres.class)
@TestPropertySource(properties = "spring.jpa.open-in-view=true")
class OsivActifTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MediaRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void unCatalogue() {
        transactionTemplate.executeWithoutResult(status -> {
            repository.deleteAllInBatch();
            Media media = new Media("Une si longue lettre", 1979);
            media.addCopy(new Copy("EX-0001"));
            media.addCopy(new Copy("EX-0002"));
            repository.save(media);
        });
    }

    @Test
    @DisplayName("OSIV actif : le controleur fautif repond quand meme")
    void osiv_actif_masque_la_lazyinitializationexception() throws Exception {
        mockMvc.perform(get("/medias/{titre}/exemplaires", "Une si longue lettre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Et le controleur correct repond aussi, evidemment")
    void le_controleur_correct_fonctionne_aussi() throws Exception {
        mockMvc.perform(get("/medias/{titre}/exemplaires-corrige", "Une si longue lettre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
