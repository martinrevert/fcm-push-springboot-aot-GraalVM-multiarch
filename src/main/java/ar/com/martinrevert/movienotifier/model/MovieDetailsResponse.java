package ar.com.martinrevert.movienotifier.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MovieDetailsResponse {

    private Data data;

    /**
     * Gets details payload section.
     *
     * @return details data
     */
    public Data getData() {
        return data;
    }

    /**
     * Sets details payload section.
     *
     * @param data details data
     */
    public void setData(Data data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {

        private Movie movie;

        /**
         * Gets movie detail object.
         *
         * @return movie details
         */
        public Movie getMovie() {
            return movie;
        }

        /**
         * Sets movie detail object.
         *
         * @param movie movie details
         */
        public void setMovie(Movie movie) {
            this.movie = movie;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Movie {

        private List<String> genres;

        @JsonProperty("language")
        private String language;

        @JsonProperty("rating")
        private Double rating;

        /**
         * Gets genres from detail payload.
         *
         * @return genres list
         */
        public List<String> getGenres() {
            return genres;
        }

        /**
         * Sets genres from detail payload.
         *
         * @param genres genres list
         */
        public void setGenres(List<String> genres) {
            this.genres = genres;
        }

        /**
         * Gets movie language.
         *
         * @return language value
         */
        public String getLanguage() {
            return language;
        }

        /**
         * Sets movie language.
         *
         * @param language language value
         */
        public void setLanguage(String language) {
            this.language = language;
        }

        /**
         * Gets movie rating.
         *
         * @return rating value
         */
        public Double getRating() {
            return rating;
        }

        /**
         * Sets movie rating.
         *
         * @param rating rating value
         */
        public void setRating(Double rating) {
            this.rating = rating;
        }
    }
}

