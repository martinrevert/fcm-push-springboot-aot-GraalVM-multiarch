package com.example.movienotifier.repository;

import com.example.movienotifier.model.NotifiedMovie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotifiedMovieRepository extends JpaRepository<NotifiedMovie, Integer> {
}


