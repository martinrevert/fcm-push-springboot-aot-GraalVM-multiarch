package ar.com.martinrevert.movienotifier.service;

import ar.com.martinrevert.movienotifier.model.MovieDetailsResponse;
import ar.com.martinrevert.movienotifier.model.MovieResponse;
import ar.com.martinrevert.movienotifier.model.NotifiedMovie;
import ar.com.martinrevert.movienotifier.repository.NotifiedMovieRepository;
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
import static org.mockito.ArgumentMatchers.isNull;
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
        MovieDetailsResponse detailsResponse = createDetailsResponse("en", 7.1);

        when(restClient.get().uri(anyString()).retrieve().body(eq(MovieResponse.class))).thenReturn(response);
        when(restClient.get().uri(eq("https://yts.bz/api/v2/movie_details.json?movie_id={movieId}"), eq(12345))
            .retrieve().body(eq(MovieDetailsResponse.class))).thenReturn(detailsResponse);
        when(notifiedMovieRepository.existsById(12345)).thenReturn(false);

        moviePollingService.pollMovies();

        InOrder inOrder = inOrder(notifiedMovieRepository, notificationService);
        inOrder.verify(notifiedMovieRepository).saveAndFlush(any(NotifiedMovie.class));
        inOrder.verify(notificationService).sendMovieNotification(eq("Test Movie"), eq(12345), isNull(), isNull(), eq("en"), eq(7.1));
        verify(notificationService).sendMovieNotification(eq("Test Movie"), eq(12345), isNull(), isNull(), eq("en"), eq(7.1));
        verify(notifiedMovieRepository).saveAndFlush(any(NotifiedMovie.class));
    }

    @Test
    void pollMoviesSkipsNotificationForAlreadyPersistedMovie() {
        MovieResponse response = createResponseWithMovie(12345, "Test Movie");

        when(restClient.get().uri(anyString()).retrieve().body(eq(MovieResponse.class))).thenReturn(response);
        when(notifiedMovieRepository.existsById(12345)).thenReturn(true);

        moviePollingService.pollMovies();

        verify(notificationService, never()).sendMovieNotification(anyString(), any(), any(), any(), any(), any());
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

        verify(notificationService, never()).sendMovieNotification(anyString(), any(), any(), any(), any(), any());
    }

    @Test
    void pollMoviesSkipsNotificationWhenMovieLanguageIsNotEnglish() {
        MovieResponse response = createResponseWithMovie(12345, "Test Movie");
        MovieDetailsResponse detailsResponse = createDetailsResponse("es", 8.0);

        when(restClient.get().uri(anyString()).retrieve().body(eq(MovieResponse.class))).thenReturn(response);
        when(restClient.get().uri(eq("https://yts.bz/api/v2/movie_details.json?movie_id={movieId}"), eq(12345))
            .retrieve().body(eq(MovieDetailsResponse.class))).thenReturn(detailsResponse);

        moviePollingService.pollMovies();

        verify(notificationService, never()).sendMovieNotification(anyString(), any(), any(), any(), any(), any());
        verify(notifiedMovieRepository).saveAndFlush(any(NotifiedMovie.class));
    }

    @Test
    void pollMoviesSkipsNotificationWhenMovieRatingIsBelowMinimum() {
        MovieResponse response = createResponseWithMovie(12345, "Test Movie");
        MovieDetailsResponse detailsResponse = createDetailsResponse("en", 5.9);

        when(restClient.get().uri(anyString()).retrieve().body(eq(MovieResponse.class))).thenReturn(response);
        when(restClient.get().uri(eq("https://yts.bz/api/v2/movie_details.json?movie_id={movieId}"), eq(12345))
            .retrieve().body(eq(MovieDetailsResponse.class))).thenReturn(detailsResponse);

        moviePollingService.pollMovies();

        verify(notificationService, never()).sendMovieNotification(anyString(), any(), any(), any(), any(), any());
        verify(notifiedMovieRepository).saveAndFlush(any(NotifiedMovie.class));
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

    private MovieDetailsResponse createDetailsResponse(String language, Double rating) {
        MovieDetailsResponse.Movie movie = new MovieDetailsResponse.Movie();
        movie.setLanguage(language);
        movie.setRating(rating);

        MovieDetailsResponse.Data data = new MovieDetailsResponse.Data();
        data.setMovie(movie);

        MovieDetailsResponse response = new MovieDetailsResponse();
        response.setData(data);
        return response;
    }
}



