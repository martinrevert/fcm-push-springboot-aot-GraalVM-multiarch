package ar.com.martinrevert.movienotifier.service;

import ar.com.martinrevert.movienotifier.model.NotifiedMovie;
import ar.com.martinrevert.movienotifier.repository.NotifiedMovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotifiedMoviesService {

    static final int DEFAULT_RECENT_DAYS_WINDOW = 15;

    private final NotifiedMovieRepository notifiedMovieRepository;
    private final int recentDaysWindow;

    /**
     * Creates the service with a configurable look-back window.
     *
     * @param notifiedMovieRepository repository for persisted notified-movie records
     * @param recentDaysWindow number of days to look back when listing recent movies
     */
    @Autowired
    public NotifiedMoviesService(
        NotifiedMovieRepository notifiedMovieRepository,
        @Value("${movie.recent.days-window:15}") int recentDaysWindow
    ) {
        this.notifiedMovieRepository = notifiedMovieRepository;
        this.recentDaysWindow = recentDaysWindow;
    }

    /**
     * Returns all movies that were notified within the last {@code movie.recent.days-window} days
     * (default: {@value #DEFAULT_RECENT_DAYS_WINDOW}), ordered from most-recently notified to oldest.
     *
     * @return list of notified movies in the recent window
     */
    public List<NotifiedMovie> getRecentlyNotifiedMovies() {
        LocalDateTime since = LocalDateTime.now().minusDays(recentDaysWindow);
        return notifiedMovieRepository.findByNotifiedAtAfterOrderByNotifiedAtDesc(since);
    }
}
