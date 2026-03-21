package com.example.movienotifier.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieResponse {
    private Data data;

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private List<Movie> movies;
        public List<Movie> getMovies() { return movies; }
        public void setMovies(List<Movie> movies) { this.movies = movies; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Movie {
        private int id;
        private String title;
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }
}
