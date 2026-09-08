package ar.com.martinrevert.movienotifier.controller;

import ar.com.martinrevert.movienotifier.model.NotifiedMovie;
import ar.com.martinrevert.movienotifier.service.NotifiedMoviesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifiedMoviesControllerTest {

    @Mock
    private NotifiedMoviesService notifiedMoviesService;

    private NotifiedMoviesController controller;

    @BeforeEach
    void setUp() {
        controller = new NotifiedMoviesController(notifiedMoviesService);
    }

    @Test
    void recentMoviesReturnsOkWithMovieList() {
        NotifiedMovie movie = new NotifiedMovie(42, "Inception");
        when(notifiedMoviesService.getRecentlyNotifiedMovies()).thenReturn(List.of(movie));

        ResponseEntity<List<NotifiedMovie>> response = controller.recentMovies();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(42, response.getBody().get(0).getMovieId());
        assertEquals("Inception", response.getBody().get(0).getTitle());
        verify(notifiedMoviesService).getRecentlyNotifiedMovies();
    }

    @Test
    void recentMoviesReturnsOkWithEmptyListWhenNoRecentMovies() {
        when(notifiedMoviesService.getRecentlyNotifiedMovies()).thenReturn(List.of());

        ResponseEntity<List<NotifiedMovie>> response = controller.recentMovies();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(notifiedMoviesService).getRecentlyNotifiedMovies();
    }

    @Test
    void recentMoviesDelegatesCompletelyToService() {
        NotifiedMovie first = new NotifiedMovie(1, "Movie One");
        NotifiedMovie second = new NotifiedMovie(2, "Movie Two");
        when(notifiedMoviesService.getRecentlyNotifiedMovies()).thenReturn(List.of(first, second));

        ResponseEntity<List<NotifiedMovie>> response = controller.recentMovies();

        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(1, response.getBody().get(0).getMovieId());
        assertEquals(2, response.getBody().get(1).getMovieId());
    }
}
