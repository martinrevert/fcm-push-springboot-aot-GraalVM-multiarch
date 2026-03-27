package ar.com.martinrevert.movienotifier.service;

import ar.com.martinrevert.movienotifier.model.MovieResponse;
import ar.com.martinrevert.movienotifier.model.MovieDetailsResponse;
import ar.com.martinrevert.movienotifier.model.NotifiedMovie;
import ar.com.martinrevert.movienotifier.repository.NotifiedMovieRepository;
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
    private static final String REQUIRED_LANGUAGE = "en";
    private static final double MIN_RATING = 6.0;

    private final RestClient restClient;
    private final NotificationService notificationService;
    private final NotifiedMovieRepository notifiedMovieRepository;

    /**
     * Creates the polling service.
     *
     * @param notificationService service used to send push notifications
     * @param restClient HTTP client used to call YTS
     * @param notifiedMovieRepository repository used for deduplication persistence
     */
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

    /**
     * Polls YTS for movies and notifies subscribers about unseen entries.
     */
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
                    String movieTitle = resolveMovieTitle(movie);

                    // Persist every unseen movie id so we do not re-evaluate it on later polls.
                    if (!tryMarkMovieAsNotified(movie)) {
                        continue;
                    }

                    MovieDetailsResponse.Movie details = fetchMovieDetails(movie.getId());
                    if (!isEligibleForNotification(details)) {
                        logger.info(
                            "Skipping movie '{}' (ID: {}) because it does not match notification filters. language='{}' rating={}",
                            movieTitle,
                            movie.getId(),
                            details != null ? details.getLanguage() : null,
                            details != null ? details.getRating() : null
                        );
                        continue;
                    }

                    notificationService.sendMovieNotification(
                        movieTitle,
                        movie.getId(),
                        resolvePosterUrl(movie),
                        details.getGenres(),
                        details.getLanguage(),
                        details.getRating()
                    );
                    logger.info("Found new eligible movie: {} (ID: {})", movieTitle, movie.getId());
                    newMoviesCount++;
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

    /**
     * Tries to mark a movie as notified, preventing duplicate sends across cycles/restarts.
     *
     * @param movie movie candidate from YTS response
     * @return true when movie is newly persisted and should be notified
     */
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

    /**
     * Resolves the best available movie title for notifications.
     *
     * @param movie movie payload from YTS
     * @return title to send in notifications
     */
    private String resolveMovieTitle(MovieResponse.Movie movie) {
        if (movie.getTitle() != null && !movie.getTitle().isBlank()) {
            return movie.getTitle();
        }
        if (movie.getTitleLong() != null && !movie.getTitleLong().isBlank()) {
            return movie.getTitleLong();
        }
        return "Movie " + movie.getId();
    }

    /**
     * Resolves poster URL from list_movies payload.
     *
     * @param movie movie payload from YTS
     * @return poster URL or null
     */
    private String resolvePosterUrl(MovieResponse.Movie movie) {
        if (movie.getLargeCoverImage() != null && !movie.getLargeCoverImage().isBlank()) {
            return movie.getLargeCoverImage();
        }
        if (movie.getMediumCoverImage() != null && !movie.getMediumCoverImage().isBlank()) {
            return movie.getMediumCoverImage();
        }
        return null;
    }

    /**
     * Fetches movie details from YTS details endpoint.
     *
     * @param movieId movie identifier
     * @return movie detail payload or null when unavailable
     */
    private MovieDetailsResponse.Movie fetchMovieDetails(Integer movieId) {
        try {
            MovieDetailsResponse detailsResponse = restClient
                .get()
                .uri("https://yts.bz/api/v2/movie_details.json?movie_id={movieId}", movieId)
                .retrieve()
                .body(MovieDetailsResponse.class);

            if (detailsResponse == null || detailsResponse.getData() == null) {
                return null;
            }
            return detailsResponse.getData().getMovie();
        } catch (Exception e) {
            logger.warn("Could not fetch movie details for id={}. Sending basic payload.", movieId);
            return null;
        }
    }

    /**
     * Determines whether a movie can be notified based on language and rating.
     *
     * @param details movie details payload
     * @return true when language is English and rating is at least 6.0
     */
    private boolean isEligibleForNotification(MovieDetailsResponse.Movie details) {
        if (details == null || details.getLanguage() == null || details.getRating() == null) {
            return false;
        }

        return REQUIRED_LANGUAGE.equalsIgnoreCase(details.getLanguage().trim())
            && details.getRating() >= MIN_RATING;
    }
}

