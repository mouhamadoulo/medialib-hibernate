package com.medialib.queries;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Tuple;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ne ramener que ce dont on a besoin. C'est la technique la plus efficace du chapitre
 * suivant, et elle commence ici.
 */
class ProjectionsEtNativeTest extends RequetesTestBase {

    @Test
    @DisplayName("Une projection record ne selectionne que les colonnes utiles")
    void une_projection_record_ne_selectionne_que_les_colonnes_utiles() {
        unCatalogue();

        inTransaction(session -> {
            sql().reset();

            List<MediaResume> resumes = session.createQuery(
                    "select new com.medialib.queries.MediaResume(m.title, m.publishedYear) "
                            + "from Media m order by m.title", MediaResume.class)
                    .getResultList();

            assertThat(sql().selectCount()).isEqualTo(1);
            // Deux colonnes seulement : ni l'identifiant, ni les auteurs.
            assertThat(sql().lastQuery()).contains("title", "published_year");
            assertThat(sql().lastQuery()).doesNotContain("join");
            assertThat(resumes).hasSize(3);
            assertThat(resumes.get(0)).isEqualTo(new MediaResume("L'Aventure ambigue", 1961));
        });
    }

    @Test
    @DisplayName("Une projection n'est pas managee : la modifier n'ecrit rien")
    void une_projection_n_est_pas_suivie_par_hibernate() {
        unCatalogue();

        inTransaction(session -> {
            session.createQuery(
                    "select new com.medialib.queries.MediaResume(m.title, m.publishedYear) "
                            + "from Media m", MediaResume.class)
                    .getResultList();
            sql().reset();

            session.flush();

            // Rien a surveiller : un record n'est pas une entite.
            assertThat(sql().updateCount()).isZero();
        });
    }

    @Test
    @DisplayName("Tuple donne un acces par position et par alias")
    void tuple_donne_un_acces_par_alias() {
        unCatalogue();

        inTransaction(session -> {
            Tuple ligne = session.createQuery(
                    "select m.title as titre, m.publishedYear as annee from Media m "
                            + "where m.publishedYear = 1979", Tuple.class)
                    .getSingleResult();

            assertThat(ligne.get("titre")).isEqualTo("Une si longue lettre");
            assertThat(ligne.get("annee")).isEqualTo(1979);
            assertThat(ligne.get(0)).isEqualTo(ligne.get("titre"));
        });
    }

    @Test
    @DisplayName("Une requete native est mappee par @SqlResultSetMapping")
    void une_requete_native_est_mappee_par_sqlresultsetmapping() {
        unCatalogue();

        inTransaction(session -> {
            @SuppressWarnings("unchecked")
            List<MediaResume> resumes =
                    session.createNamedQuery("Media.resumesNatifs").getResultList();

            assertThat(resumes).hasSize(3);
            assertThat(resumes.get(0)).isInstanceOf(MediaResume.class);
            assertThat(resumes.get(0).title()).isEqualTo("L'Aventure ambigue");
        });
    }

    @Test
    @DisplayName("Une requete nommee est validee au demarrage, pas a la premiere execution")
    void une_namedquery_est_disponible_et_typee() {
        unCatalogue();

        inTransaction(session -> {
            List<Media> de1979 = session.createNamedQuery("Media.parAnnee", Media.class)
                    .setParameter("annee", 1979)
                    .getResultList();

            assertThat(de1979).extracting(Media::getTitle)
                    .containsExactly("Une si longue lettre");
        });

        // Le vrai interet : la SessionFactory de ce test n'aurait pas demarre si l'une des
        // requetes nommees de Media avait ete fautive. L'erreur arrive au build, pas en prod.
        assertThat(sessionFactory().isOpen()).isTrue();
    }

    @Test
    @DisplayName("Une requete nommee peut aussi projeter dans un record")
    void une_namedquery_peut_projeter() {
        unCatalogue();

        inTransaction(session -> {
            List<MediaResume> resumes =
                    session.createNamedQuery("Media.resumes", MediaResume.class).getResultList();

            assertThat(resumes).hasSize(3);
        });
    }
}
