package com.example.movienotifier.service;

import com.example.movienotifier.model.MovieResponse;
import com.example.movienotifier.model.NotifiedMovie;
import com.example.movienotifier.repository.NotifiedMovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class MoviePollingService {

    private static final Logger logger = LoggerFactory.getLogger(MoviePollingService.class);

    private final RestClient restClient;
    private final NotificationService notificationService;
    private final NotifiedMovieRepository notifiedMovieRepository;

    @Autowired
    public MoviePollingService(
        NotificationService notificationService,
        RestClient restClient,
        NotifiedMovieRepository notifiedMovieRepository
    ) {
        this.restClient = restClient;
        this.notificationService = notificationService;
        this.notifiedMovieRepository = notifiedMovieRepository;
    }

    @Scheduled(fixedRateString = "${movie.polling.fixed-rate-ms:60000}")
    public void pollMovies() {
        logger.info("Polling for new movies...");
        String url = "https://yts.ag/api/v2/list_movies.json";
        try {
            MovieResponse response = restClient
                .get()
                .uri(url)
                .retrieve()
                .body(MovieResponse.class);

            if (response != null) {
                logger.info(
                    "YTS payload status='{}' message='{}'",
                    response.getStatus(),
                    response.getStatusMessage()
                );
            }

            if (response != null && response.getData() != null && response.getData().getMovies() != null) {
                int totalMovies = response.getData().getMovies().size();
                logger.info(
                    "YTS response parsed successfully. movieCount={} moviesInResponse={}",
                    response.getData().getMovieCount(),
                    totalMovies
                );
                int newMoviesCount = 0;
                for (MovieResponse.Movie movie : response.getData().getMovies()) {
                    if (tryMarkMovieAsNotified(movie)) {
                        String movieTitle = resolveMovieTitle(movie);
                        notificationService.sendMovieNotification(movieTitle);
                        logger.info("Found new movie: {} (ID: {})", movieTitle, movie.getId());
                        newMoviesCount++;
                    }
                }
                if (newMoviesCount == 0) {
                    logger.info("No new movies found.");
                } else {
                    logger.info("Finished polling. Found {} new movies.", newMoviesCount);
                }
            } else {
                logger.warn("Movie API response was null or empty.");
            }
        } catch (Exception e) {
            logger.error("Error while polling for movies", e);
        }
    }

    private boolean tryMarkMovieAsNotified(MovieResponse.Movie movie) {
        Integer movieId = movie.getId();
        if (notifiedMovieRepository.existsById(movieId)) {
            return false;
        }

        try {
            // Persist first so notified_movies remains the source of truth across restarts.
            notifiedMovieRepository.saveAndFlush(new NotifiedMovie(movieId, resolveMovieTitle(movie)));
            return true;
        } catch (DataIntegrityViolationException e) {
            logger.debug("Movie already claimed by another poll cycle: {}", movieId);
            return false;
        }
    }

    private String resolveMovieTitle(MovieResponse.Movie movie) {
        if (movie.getTitle() != null && !movie.getTitle().isBlank()) {
            return movie.getTitle();
        }
        if (movie.getTitleLong() != null && !movie.getTitleLong().isBlank()) {
            return movie.getTitleLong();
        }
        return "Movie " + movie.getId();
    }
}
