package com.example.movienotifier.service;

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

    public NotifiedMovie() {
        this.notifiedAt = LocalDateTime.now();
    }

    public NotifiedMovie(Integer movieId, String title) {
        this.movieId = movieId;
        this.title = title;
        this.notifiedAt = LocalDateTime.now();
    }

    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getNotifiedAt() {
        return notifiedAt;
    }

    public void setNotifiedAt(LocalDateTime notifiedAt) {
        this.notifiedAt = notifiedAt;
    }
}

