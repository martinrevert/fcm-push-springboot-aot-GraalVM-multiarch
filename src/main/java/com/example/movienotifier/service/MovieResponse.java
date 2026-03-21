package com.example.movienotifier.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieResponse {

    private String status;

    @JsonProperty("status_message")
    private String statusMessage;

    private Data data;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {

        @JsonProperty("movie_count")
        private Integer movieCount;

        private Integer limit;

        @JsonProperty("page_number")
        private Integer pageNumber;

        private List<Movie> movies;

        public Integer getMovieCount() { return movieCount; }
        public void setMovieCount(Integer movieCount) { this.movieCount = movieCount; }

        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }

        public Integer getPageNumber() { return pageNumber; }
        public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

        public List<Movie> getMovies() { return movies; }
        public void setMovies(List<Movie> movies) { this.movies = movies; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Movie {

        private int id;
        private String title;

        @JsonProperty("title_long")
        private String titleLong;

        private Integer year;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getTitleLong() { return titleLong; }
        public void setTitleLong(String titleLong) { this.titleLong = titleLong; }

        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
    }
}
