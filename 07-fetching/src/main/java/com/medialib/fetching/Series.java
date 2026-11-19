package com.medialib.fetching;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * Une serie et ses episodes — le pretexte a la troisieme strategie de chargement de collection.
 *
 * <p>Cette entite est separee de {@link Media} a dessein : {@code Media} porte un
 * {@code @BatchSize} dont le chapitre assertionne precisement l'effet, et lui superposer une
 * autre strategie brouillerait les deux lecons.
 *
 * <p>{@code @Fetch(SUBSELECT)} ne regroupe pas par identifiants comme {@code @BatchSize} :
 * quand on touche la collection d'<em>une</em> des entites ramenees par une requete, Hibernate
 * charge les collections de <strong>toutes</strong> en rejouant cette requete en sous-requete.
 *
 * <p>D'ou ses deux caracteristiques, demontrees par {@code FetchSubselectTest} :
 *
 * <ul>
 *   <li>aucune taille de paquet a regler, et aucune liste d'identifiants a transporter — mais
 *       la requete d'origine est <strong>payee deux fois</strong>, filtres et jointures
 *       compris ;
 *   <li>elle n'a d'effet que sur des entites ramenees par une requete. Chargees une a une par
 *       {@code find}, il n'y a aucune requete a rejouer et le N+1 revient intact — la ou
 *       {@code @BatchSize} aurait regroupe.
 * </ul>
 */
@Entity
public class Series {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "series_seq")
    @SequenceGenerator(name = "series_seq", sequenceName = "series_seq", allocationSize = 1)
    private Long id;

    private String title;

    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    private List<Episode> episodes = new ArrayList<>();

    protected Series() {
    }

    public Series(String title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }

    public void addEpisode(Episode episode) {
        episodes.add(episode);
        episode.setSeries(this);
    }
}
