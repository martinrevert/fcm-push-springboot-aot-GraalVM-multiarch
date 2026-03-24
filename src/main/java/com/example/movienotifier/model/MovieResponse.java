package com.example.movienotifier.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieResponse {

    private String status;

    @JsonProperty("status_message")
    private String statusMessage;

    private Data data;

    /**
     * Gets YTS response status.
     *
     * @return status value
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets YTS response status.
     *
     * @param status status value
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets YTS response status message.
     *
     * @return status message
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * Sets YTS response status message.
     *
     * @param statusMessage status message
     */
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    /**
     * Gets payload data section.
     *
     * @return response data
     */
    public Data getData() {
        return data;
    }

    /**
     * Sets payload data section.
     *
     * @param data response data
     */
    public void setData(Data data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {

        @JsonProperty("movie_count")
        private Integer movieCount;

        private Integer limit;

        @JsonProperty("page_number")
        private Integer pageNumber;

        private List<Movie> movies;

        /**
         * Gets total movie count for query.
         *
         * @return movie count
         */
        public Integer getMovieCount() {
            return movieCount;
        }

        /**
         * Sets total movie count for query.
         *
         * @param movieCount movie count
         */
        public void setMovieCount(Integer movieCount) {
            this.movieCount = movieCount;
        }

        /**
         * Gets page size limit.
         *
         * @return page size
         */
        public Integer getLimit() {
            return limit;
        }

        /**
         * Sets page size limit.
         *
         * @param limit page size
         */
        public void setLimit(Integer limit) {
            this.limit = limit;
        }

        /**
         * Gets current page number.
         *
         * @return page number
         */
        public Integer getPageNumber() {
            return pageNumber;
        }

        /**
         * Sets current page number.
         *
         * @param pageNumber page number
         */
        public void setPageNumber(Integer pageNumber) {
            this.pageNumber = pageNumber;
        }

        /**
         * Gets movies for current page.
         *
         * @return list of movies
         */
        public List<Movie> getMovies() {
            return movies;
        }

        /**
         * Sets movies for current page.
         *
         * @param movies list of movies
         */
        public void setMovies(List<Movie> movies) {
            this.movies = movies;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Movie {

        private int id;
        private String title;

        @JsonProperty("title_long")
        private String titleLong;

        private Integer year;

        /**
         * Gets movie id.
         *
         * @return movie id
         */
        public int getId() {
            return id;
        }

        /**
         * Sets movie id.
         *
         * @param id movie id
         */
        public void setId(int id) {
            this.id = id;
        }

        /**
         * Gets short movie title.
         *
         * @return title
         */
        public String getTitle() {
            return title;
        }

        /**
         * Sets short movie title.
         *
         * @param title title
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * Gets long movie title.
         *
         * @return long title
         */
        public String getTitleLong() {
            return titleLong;
        }

        /**
         * Sets long movie title.
         *
         * @param titleLong long title
         */
        public void setTitleLong(String titleLong) {
            this.titleLong = titleLong;
        }

        /**
         * Gets release year.
         *
         * @return release year
         */
        public Integer getYear() {
            return year;
        }

        /**
         * Sets release year.
         *
         * @param year release year
         */
        public void setYear(Integer year) {
            this.year = year;
        }
    }
}

