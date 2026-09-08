package ar.com.martinrevert.movienotifier.repository;

import ar.com.martinrevert.movienotifier.model.NotifiedMovie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotifiedMovieRepository extends JpaRepository<NotifiedMovie, Integer> {

    /**
     * Returns all notified-movie records whose {@code notifiedAt} timestamp is strictly
     * after the given cutoff, ordered from newest to oldest.
     *
     * @param since earliest (exclusive) timestamp to include
     * @return list of matching records
     */
    List<NotifiedMovie> findByNotifiedAtAfterOrderByNotifiedAtDesc(LocalDateTime since);
}



