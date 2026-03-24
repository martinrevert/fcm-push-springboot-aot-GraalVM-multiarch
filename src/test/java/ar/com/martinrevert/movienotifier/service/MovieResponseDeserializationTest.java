package ar.com.martinrevert.movienotifier.service;

import ar.com.martinrevert.movienotifier.model.MovieResponse;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MovieResponseDeserializationTest {

    @Test
    void shouldParseYtsListMoviesPayload() throws Exception {
        String json = """
            {
              "status": "ok",
              "status_message": "Query was successful",
              "data": {
                "movie_count": 73910,
                "limit": 2,
                "page_number": 1,
                "movies": [
                  {
                    "id": 75189,
                    "title": "Fez Summer '55",
                    "title_long": "Fez Summer '55 (2023)",
                    "medium_cover_image": "https://img.example/medium.jpg",
                    "large_cover_image": "https://img.example/large.jpg",
                    "year": 2023
                  },
                  {
                    "id": 75188,
                    "title": "The Art of Nothing",
                    "title_long": "The Art of Nothing (2024)",
                    "year": 2024
                  }
                ]
              }
            }
            """;

        JsonMapper mapper = JsonMapper.builder().build();
        MovieResponse response = mapper.readValue(json, MovieResponse.class);

        assertNotNull(response);
        assertEquals("ok", response.getStatus());
        assertEquals("Query was successful", response.getStatusMessage());
        assertNotNull(response.getData());
        assertEquals(73910, response.getData().getMovieCount());
        assertEquals(2, response.getData().getLimit());
        assertEquals(1, response.getData().getPageNumber());
        assertNotNull(response.getData().getMovies());
        assertEquals(2, response.getData().getMovies().size());
        assertEquals(75189, response.getData().getMovies().get(0).getId());
        assertEquals("Fez Summer '55", response.getData().getMovies().get(0).getTitle());
        assertEquals("https://img.example/medium.jpg", response.getData().getMovies().get(0).getMediumCoverImage());
        assertEquals("https://img.example/large.jpg", response.getData().getMovies().get(0).getLargeCoverImage());
    }
}

