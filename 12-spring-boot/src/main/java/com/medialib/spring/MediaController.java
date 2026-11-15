package com.medialib.spring;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Un controleur volontairement minimal, et volontairement <strong>mal ecrit</strong>.
 *
 * <p>Il existe pour une seule raison : rendre l'OSIV visible. Il recupere une oeuvre par le
 * service, puis touche sa collection paresseuse <em>apres</em> que la transaction du service
 * est terminee.
 *
 * <ul>
 *   <li>Avec {@code spring.jpa.open-in-view=true} (le defaut de Spring Boot), la session est
 *       maintenue ouverte par un filtre jusqu'a la fin du rendu HTTP : ca marche. Le probleme
 *       est masque, et les requetes partent depuis la couche de presentation.</li>
 *   <li>Avec {@code false}, on obtient une {@code LazyInitializationException} : le vrai
 *       etat du code apparait.</li>
 * </ul>
 *
 * <p>Ce n'est donc pas l'OSIV qui est en cause, c'est ce controleur. L'OSIV ne fait que
 * dissimuler le defaut — et il le paie en tenant une connexion de base pendant toute la
 * duree du rendu.
 */
@RestController
public class MediaController {

    private final MediaService service;

    public MediaController(MediaService service) {
        this.service = service;
    }

    /** Variante fautive : la collection est touchee hors transaction. */
    @GetMapping("/medias/{titre}/exemplaires")
    public ResponseEntity<List<String>> exemplaires(@PathVariable String titre) {
        Media media = service.parTitre(titre).orElse(null);
        if (media == null) {
            return ResponseEntity.notFound().build();
        }
        // Ici, la transaction du service est fermee. Sans OSIV, cette ligne echoue.
        List<String> codes = media.getCopies().stream().map(Copy::getBarcode).toList();
        return ResponseEntity.ok(codes);
    }

    /** Variante correcte : le service ramene tout ce dont la vue a besoin. */
    @GetMapping("/medias/{titre}/exemplaires-corrige")
    public ResponseEntity<List<String>> exemplairesCorrige(@PathVariable String titre) {
        return service.parTitre(titre)
                .map(media -> ResponseEntity.ok(service.codesBarresDe(media.getId())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
