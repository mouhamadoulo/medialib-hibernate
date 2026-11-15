package com.medialib.spring;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import java.util.ArrayList;
import java.util.List;

/**
 * Une oeuvre du catalogue.
 *
 * <p>Exactement la meme entite qu'aux chapitres 03 et 07 : c'est bien Hibernate qui fait le
 * travail, Spring ne change rien au mapping. La collection reste LAZY, et c'est elle qui sert
 * a demontrer l'OSIV.
 */
@Entity
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_seq")
    @SequenceGenerator(name = "media_seq", sequenceName = "media_seq", allocationSize = 1)
    private Long id;

    private String title;

    private Integer publishedYear;

    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL)
    private List<Copy> copies = new ArrayList<>();

    protected Media() {
    }

    public Media(String title, Integer publishedYear) {
        this.title = title;
        this.publishedYear = publishedYear;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getPublishedYear() {
        return publishedYear;
    }

    public List<Copy> getCopies() {
        return copies;
    }

    public void addCopy(Copy copy) {
        copies.add(copy);
        copy.setMedia(this);
    }
}
