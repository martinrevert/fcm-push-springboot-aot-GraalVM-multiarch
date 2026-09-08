package ar.com.martinrevert.movienotifier.service;

import ar.com.martinrevert.movienotifier.model.NotifiedMovie;
import ar.com.martinrevert.movienotifier.repository.NotifiedMovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifiedMoviesServiceTest {

    @Mock
    private NotifiedMovieRepository notifiedMovieRepository;

    private NotifiedMoviesService service;

    @BeforeEach
    void setUp() {
        service = new NotifiedMoviesService(notifiedMovieRepository);
    }

    @Test
    void getRecentlyNotifiedMoviesReturnsRepositoryResults() {
        NotifiedMovie movie = new NotifiedMovie(1, "Test Movie");
        when(notifiedMovieRepository.findByNotifiedAtAfterOrderByNotifiedAtDesc(any(LocalDateTime.class)))
            .thenReturn(List.of(movie));

        List<NotifiedMovie> result = service.getRecentlyNotifiedMovies();

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getMovieId());
        assertEquals("Test Movie", result.get(0).getTitle());
    }

    @Test
    void getRecentlyNotifiedMoviesPassesCutoffWithin15DayWindow() {
        when(notifiedMovieRepository.findByNotifiedAtAfterOrderByNotifiedAtDesc(any(LocalDateTime.class)))
            .thenReturn(List.of());

        LocalDateTime before = LocalDateTime.now().minusDays(NotifiedMoviesService.RECENT_DAYS_WINDOW).minusSeconds(5);

        service.getRecentlyNotifiedMovies();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notifiedMovieRepository).findByNotifiedAtAfterOrderByNotifiedAtDesc(captor.capture());

        LocalDateTime cutoff = captor.getValue();
        // The cutoff should be approximately now-15days (within a 10-second tolerance)
        assertTrue(cutoff.isAfter(before), "Cutoff should be after now-15days-5s");
        assertTrue(cutoff.isBefore(LocalDateTime.now().minusDays(NotifiedMoviesService.RECENT_DAYS_WINDOW).plusSeconds(5)),
            "Cutoff should be before now-15days+5s");
    }

    @Test
    void getRecentlyNotifiedMoviesReturnsEmptyListWhenNoneFound() {
        when(notifiedMovieRepository.findByNotifiedAtAfterOrderByNotifiedAtDesc(any(LocalDateTime.class)))
            .thenReturn(List.of());

        List<NotifiedMovie> result = service.getRecentlyNotifiedMovies();

        assertTrue(result.isEmpty());
    }

    @Test
    void getRecentlyNotifiedMoviesReturnsMultipleMoviesInOrder() {
        NotifiedMovie newer = new NotifiedMovie(2, "Newer Movie");
        NotifiedMovie older = new NotifiedMovie(1, "Older Movie");
        when(notifiedMovieRepository.findByNotifiedAtAfterOrderByNotifiedAtDesc(any(LocalDateTime.class)))
            .thenReturn(List.of(newer, older));

        List<NotifiedMovie> result = service.getRecentlyNotifiedMovies();

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getMovieId());
        assertEquals(1, result.get(1).getMovieId());
    }
}
