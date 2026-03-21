package com.example.movienotifier.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class MoviePollingService {

    private static final Logger logger = LoggerFactory.getLogger(MoviePollingService.class);

    private final RestClient restClient;
    private final Set<Integer> seenMovieIds;
    private final NotificationService notificationService;

    @Autowired
    public MoviePollingService(NotificationService notificationService, RestClient restClient) {
        this.restClient = restClient;
        this.seenMovieIds = new HashSet<>();
        this.notificationService = notificationService;
    }

    @Scheduled(fixedRate = 60000) // every 60 seconds
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
                    if (!seenMovieIds.contains(movie.getId())) {
                        seenMovieIds.add(movie.getId());
                        notificationService.sendMovieNotification(movie.getTitle());
                        logger.info("Found new movie: {} (ID: {})", movie.getTitle(), movie.getId());
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
}
