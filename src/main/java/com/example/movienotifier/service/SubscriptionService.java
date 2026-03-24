package com.example.movienotifier.service;

import com.example.movienotifier.model.Subscription;
import com.example.movienotifier.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;

    @Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public Subscription subscribe(String registrationToken) {
        String normalizedToken = normalizeToken(registrationToken);
        Optional<Subscription> existingSubscription = subscriptionRepository.findByRegistrationToken(normalizedToken);
        if (existingSubscription.isPresent()) {
            logger.info("Registration token already exists: {}", normalizedToken);
            return existingSubscription.get();
        } else {
            Subscription newSubscription = new Subscription(normalizedToken);
            try {
                Subscription savedSubscription = subscriptionRepository.save(newSubscription);
                logger.info("New subscription added: {}", savedSubscription);
                return savedSubscription;
            } catch (DataIntegrityViolationException e) {
                // Another request inserted the same token first. Treat as idempotent success.
                return subscriptionRepository.findByRegistrationToken(normalizedToken)
                    .map(subscription -> {
                        logger.info("Registration token already existed after concurrent insert: {}", normalizedToken);
                        return subscription;
                    })
                    .orElseThrow(() -> e);
            }
        }
    }

    public void unsubscribe(String registrationToken) {
        String normalizedToken = normalizeToken(registrationToken);
        subscriptionRepository.findByRegistrationToken(normalizedToken).ifPresent(subscription -> {
            subscriptionRepository.delete(subscription);
            logger.info("Subscription removed: {}", normalizedToken);
        });
    }

    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    private String normalizeToken(String registrationToken) {
        if (registrationToken == null || registrationToken.isBlank()) {
            throw new IllegalArgumentException("FCM registration token must not be null or blank");
        }
        // Keep token unchanged to preserve exact bytes received from the client.
        return registrationToken;
    }
}
