package com.example.movienotifier.repository;

import com.example.movienotifier.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    /**
     * Finds a subscription by its registration token.
     *
     * @param registrationToken FCM registration token
     * @return optional subscription matching the token
     */
    Optional<Subscription> findByRegistrationToken(String registrationToken);
}

