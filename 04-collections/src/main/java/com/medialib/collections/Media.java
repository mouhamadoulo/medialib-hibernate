package com.medialib.collections;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.SequenceGenerator;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SortNatural;

/**
 * Une oeuvre du catalogue, et ses six collections.
 *
 * <p>Chacune illustre une facon differente de ranger plusieurs valeurs — et chacune a un cout
 * SQL different a la modification. C'est tout l'objet du chapitre.
 */
@Entity
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_seq")
    @SequenceGenerator(name = "media_seq", sequenceName = "media_seq", allocationSize = 1)
    private Long id;

    private String title;

    /** <em>Bag</em> : une liste sans index. Hibernate ne sait pas ou se trouve un element. */
    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL)
    private List<Copy> copies = new ArrayList<>();

    /**
     * Second <em>bag</em>. Deux bags charges en {@code join fetch} dans la meme requete
     * provoquent une {@code MultipleBagFetchException} : le produit cartesien rendrait les
     * deux listes incomptables. Voir {@code MultipleBagFetchTest}.
     *
     * <p>{@code @OrderBy} fait trier la base. {@code @BatchSize} regroupe le chargement
     * paresseux de plusieurs oeuvres.
     */
    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL)
    @OrderBy("rating desc")
    @BatchSize(size = 5)
    private List<Review> reviews = new ArrayList<>();

    /**
     * <em>Bag</em> d'elements de valeur. Hibernate ne pouvant identifier une ligne, toute
     * modification se solde par un « on efface tout et on reinsere ».
     */
    @ElementCollection
    @CollectionTable(name = "media_note", joinColumns = @JoinColumn(name = "media_id"))
    @Column(name = "note")
    private List<String> notes = new ArrayList<>();

    /**
     * Liste <strong>indexee</strong> : la colonne {@code position} donne a Hibernate de quoi
     * viser une ligne precise. La modification devient ciblee.
     */
    @ElementCollection
    @CollectionTable(name = "media_agenda", joinColumns = @JoinColumn(name = "media_id"))
    @OrderColumn(name = "position")
    @Column(name = "entry")
    private List<String> agenda = new ArrayList<>();

    /** Tri en <strong>memoire</strong>, par {@code @SortNatural} : aucun ORDER BY en SQL. */
    @ElementCollection
    @CollectionTable(name = "media_tag", joinColumns = @JoinColumn(name = "media_id"))
    @Column(name = "tag")
    @SortNatural
    private SortedSet<String> tags = new TreeSet<>();

    /** Une Map : la cle devient une colonne de la table de collection. */
    @ElementCollection
    @CollectionTable(name = "media_title", joinColumns = @JoinColumn(name = "media_id"))
    @MapKeyColumn(name = "language")
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "translated_title")
    private Map<Language, String> titles = new EnumMap<>(Language.class);

    protected Media() {
    }

    public Media(String title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<Copy> getCopies() {
        return copies;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public List<String> getNotes() {
        return notes;
    }

    public List<String> getAgenda() {
        return agenda;
    }

    public SortedSet<String> getTags() {
        return tags;
    }

    public Map<Language, String> getTitles() {
        return titles;
    }

    public void addCopy(Copy copy) {
        copies.add(copy);
        copy.setMedia(this);
    }

    public void addReview(Review review) {
        reviews.add(review);
        review.setMedia(this);
    }
}
