package ar.com.martinrevert.movienotifier.repository;

import ar.com.martinrevert.movienotifier.model.NotifiedMovie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotifiedMovieRepository extends JpaRepository<NotifiedMovie, Integer> {
}



