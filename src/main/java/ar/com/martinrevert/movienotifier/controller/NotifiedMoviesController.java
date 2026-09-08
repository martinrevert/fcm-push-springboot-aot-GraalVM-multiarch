package ar.com.martinrevert.movienotifier.controller;

import ar.com.martinrevert.movienotifier.model.NotifiedMovie;
import ar.com.martinrevert.movienotifier.service.NotifiedMoviesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class NotifiedMoviesController {

    private final NotifiedMoviesService notifiedMoviesService;

    /**
     * Creates the controller.
     *
     * @param notifiedMoviesService service providing recently notified movies
     */
    @Autowired
    public NotifiedMoviesController(NotifiedMoviesService notifiedMoviesService) {
        this.notifiedMoviesService = notifiedMoviesService;
    }

    /**
     * Returns a JSON list of all movies notified within the last 15 days, ordered from
     * most-recently notified to oldest.
     *
     * <p>Each entry includes {@code movieId}, {@code title}, and {@code notifiedAt} fields.
     *
     * @return {@code 200 OK} with the list (may be empty when no movies were notified recently)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<NotifiedMovie>> recentMovies() {
        List<NotifiedMovie> movies = notifiedMoviesService.getRecentlyNotifiedMovies();
        return ResponseEntity.ok(movies);
    }
}
