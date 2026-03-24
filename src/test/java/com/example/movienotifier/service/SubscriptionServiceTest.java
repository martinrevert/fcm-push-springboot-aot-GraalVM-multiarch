package com.example.movienotifier.service;

import com.example.movienotifier.model.Subscription;
import com.example.movienotifier.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(subscriptionRepository);
    }

    @Test
    void subscribeReturnsExistingSubscriptionWhenTokenAlreadyExists() {
        Subscription existing = new Subscription("token-1");
        existing.setId(10L);

        when(subscriptionRepository.findByRegistrationToken("token-1")).thenReturn(Optional.of(existing));

        Subscription result = subscriptionService.subscribe("token-1");

        assertSame(existing, result);
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void subscribeSavesAndReturnsSubscriptionWhenTokenIsNew() {
        Subscription saved = new Subscription("token-2");
        saved.setId(11L);

        when(subscriptionRepository.findByRegistrationToken("token-2")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(saved);

        Subscription result = subscriptionService.subscribe("token-2");

        assertSame(saved, result);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void subscribeReturnsExistingSubscriptionWhenConcurrentInsertHappens() {
        Subscription existing = new Subscription("token-3");
        existing.setId(12L);

        when(subscriptionRepository.findByRegistrationToken("token-3"))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any(Subscription.class)))
            .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        Subscription result = subscriptionService.subscribe("token-3");

        assertSame(existing, result);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void subscribeRethrowsIntegrityErrorWhenTokenStillCannotBeLoaded() {
        DataIntegrityViolationException expected = new DataIntegrityViolationException("Duplicate key");

        when(subscriptionRepository.findByRegistrationToken("token-4")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenThrow(expected);

        DataIntegrityViolationException thrown = assertThrows(
            DataIntegrityViolationException.class,
            () -> subscriptionService.subscribe("token-4")
        );

        assertEquals(expected, thrown);
    }
}

