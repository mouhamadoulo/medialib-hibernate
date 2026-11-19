package com.medialib.fetching;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;

/** Un episode, cote proprietaire de l'association avec {@link Series}. */
@Entity
public class Episode {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "episode_seq")
    @SequenceGenerator(name = "episode_seq", sequenceName = "episode_seq", allocationSize = 1)
    private Long id;

    private String title;

    @ManyToOne
    @JoinColumn(name = "series_id")
    private Series series;

    protected Episode() {
    }

    public Episode(String title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Series getSeries() {
        return series;
    }

    void setSeries(Series series) {
        this.series = series;
    }
}
