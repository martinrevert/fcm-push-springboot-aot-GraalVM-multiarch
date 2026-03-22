package com.example.movienotifier.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoviePollingServicePersistenceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient restClient;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotifiedMovieRepository notifiedMovieRepository;

    private MoviePollingService moviePollingService;

    @BeforeEach
    void setUp() {
        moviePollingService = new MoviePollingService(notificationService, restClient, notifiedMovieRepository);
    }

    @Test
    void pollMoviesSavesMovieAndSendsNotificationForUnseenMovie() {
        MovieResponse response = createResponseWithMovie(12345, "Test Movie");

        when(restClient.get().uri(anyString()).retrieve().body(eq(MovieResponse.class))).thenReturn(response);
        when(notifiedMovieRepository.existsById(12345)).thenReturn(false);

        moviePollingService.pollMovies();

        InOrder inOrder = inOrder(notifiedMovieRepository, notificationService);
        inOrder.verify(notifiedMovieRepository).saveAndFlush(any(NotifiedMovie.class));
        inOrder.verify(notificationService).sendMovieNotification("Test Movie");
        verify(notificationService).sendMovieNotification("Test Movie");
        verify(notifiedMovieRepository).saveAndFlush(any(NotifiedMovie.class));
    }

    @Test
    void pollMoviesSkipsNotificationForAlreadyPersistedMovie() {
        MovieResponse response = createResponseWithMovie(12345, "Test Movie");

        when(restClient.get().uri(anyString()).retrieve().body(eq(MovieResponse.class))).thenReturn(response);
        when(notifiedMovieRepository.existsById(12345)).thenReturn(true);

        moviePollingService.pollMovies();

        verify(notificationService, never()).sendMovieNotification(anyString());
        verify(notifiedMovieRepository, never()).saveAndFlush(any(NotifiedMovie.class));
    }

    @Test
    void pollMoviesSkipsNotificationWhenMovieIsClaimedConcurrently() {
        MovieResponse response = createResponseWithMovie(12345, "Test Movie");

        when(restClient.get().uri(anyString()).retrieve().body(eq(MovieResponse.class))).thenReturn(response);
        when(notifiedMovieRepository.existsById(12345)).thenReturn(false);
        when(notifiedMovieRepository.saveAndFlush(any(NotifiedMovie.class)))
            .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        moviePollingService.pollMovies();

        verify(notificationService, never()).sendMovieNotification(anyString());
    }

    private MovieResponse createResponseWithMovie(int id, String title) {
        MovieResponse.Movie movie = new MovieResponse.Movie();
        movie.setId(id);
        movie.setTitle(title);

        MovieResponse.Data data = new MovieResponse.Data();
        data.setMovieCount(1);
        data.setMovies(List.of(movie));

        MovieResponse response = new MovieResponse();
        response.setStatus("ok");
        response.setStatusMessage("Query was successful");
        response.setData(data);

        return response;
    }
}


