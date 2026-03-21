package com.example.movienotifier.controller;

import com.example.movienotifier.service.Subscription;
import com.example.movienotifier.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService subscriptionService;

    private SubscriptionController controller;

    @BeforeEach
    void setUp() {
        controller = new SubscriptionController(subscriptionService);
    }

    @Test
    void subscribeReturnsOkWithSubscriptionBody() {
        Subscription subscription = new Subscription("token-123");
        subscription.setId(1L);
        subscription.setSubscribedAt(LocalDateTime.now());

        when(subscriptionService.subscribe("token-123")).thenReturn(subscription);

        ResponseEntity<Subscription> response = controller.subscribe("token-123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("token-123", response.getBody().getRegistrationToken());
        verify(subscriptionService).subscribe("token-123");
    }

    @Test
    void unsubscribeReturnsNoContent() {
        ResponseEntity<Void> response = controller.unsubscribe("token-123");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(subscriptionService).unsubscribe("token-123");
    }

    @Test
    void subscribeDelegatesTokenAsRequestParam() {
        Subscription subscription = new Subscription("another-token");
        when(subscriptionService.subscribe("another-token")).thenReturn(subscription);

        ResponseEntity<Subscription> response = controller.subscribe("another-token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(subscriptionService).subscribe("another-token");
    }
}


