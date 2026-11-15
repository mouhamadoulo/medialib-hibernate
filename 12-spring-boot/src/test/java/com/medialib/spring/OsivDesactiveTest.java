package com.medialib.spring;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hibernate.LazyInitializationException;
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
 * OSIV <strong>desactive</strong> — ce qu'il faut mettre en production.
 *
 * <p>Le meme controleur fautif echoue maintenant, avec la {@code LazyInitializationException}
 * du chapitre 07. Ce n'est pas une regression : c'est le defaut qui apparait enfin.
 *
 * <p>La lecon du chapitre tient en une phrase : desactiver l'OSIV ne casse pas une
 * application correcte, il revele les endroits ou elle ne l'etait pas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ConteneurPostgres.class)
@TestPropertySource(properties = "spring.jpa.open-in-view=false")
class OsivDesactiveTest {

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
    @DisplayName("OSIV desactive : le controleur fautif leve LazyInitializationException")
    void osiv_desactive_revele_le_probleme_a_corriger() {
        assertThatThrownBy(() -> mockMvc
                .perform(get("/medias/{titre}/exemplaires", "Une si longue lettre")))
                .hasRootCauseInstanceOf(LazyInitializationException.class);
    }

    @Test
    @DisplayName("Le controleur corrige passe : le service ramene des donnees deja plates")
    void le_controleur_corrige_fonctionne_sans_osiv() throws Exception {
        mockMvc.perform(get("/medias/{titre}/exemplaires-corrige", "Une si longue lettre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
