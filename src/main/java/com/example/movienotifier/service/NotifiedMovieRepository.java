package com.example.movienotifier.service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotifiedMovieRepository extends JpaRepository<NotifiedMovie, Integer> {
    boolean existsByMovieId(Integer movieId);
}

