package com.example.movienotifier.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notified_movies")
public class NotifiedMovie {

    @Id
    private Integer movieId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime notifiedAt;

    /**
     * Creates an empty notified movie record with current timestamp.
     */
    public NotifiedMovie() {
        this.notifiedAt = LocalDateTime.now();
    }

    /**
     * Creates a notified movie record with id, title, and current timestamp.
     *
     * @param movieId movie identifier
     * @param title movie title
     */
    public NotifiedMovie(Integer movieId, String title) {
        this.movieId = movieId;
        this.title = title;
        this.notifiedAt = LocalDateTime.now();
    }

    /**
     * Gets movie identifier.
     *
     * @return movie id
     */
    public Integer getMovieId() {
        return movieId;
    }

    /**
     * Sets movie identifier.
     *
     * @param movieId movie id
     */
    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    /**
     * Gets movie title.
     *
     * @return movie title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets movie title.
     *
     * @param title movie title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets notification timestamp.
     *
     * @return notification time
     */
    public LocalDateTime getNotifiedAt() {
        return notifiedAt;
    }

    /**
     * Sets notification timestamp.
     *
     * @param notifiedAt notification time
     */
    public void setNotifiedAt(LocalDateTime notifiedAt) {
        this.notifiedAt = notifiedAt;
    }
}


