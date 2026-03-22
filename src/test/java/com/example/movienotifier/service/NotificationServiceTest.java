package com.example.movienotifier.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(subscriptionService, firebaseMessaging);
    }

    @Test
    void sendMovieNotificationSkipsWhenNoSubscriptions() throws Exception {
        when(subscriptionService.getAllSubscriptions()).thenReturn(List.of());

        notificationService.sendMovieNotification("Movie");

        verify(firebaseMessaging, never()).send(any());
    }

    @Test
    void sendMovieNotificationRemovesSubscriptionForUnregisteredToken() throws Exception {
        Subscription subscription = new Subscription("dead-token");
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);

        when(subscriptionService.getAllSubscriptions()).thenReturn(List.of(subscription));
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(firebaseMessaging.send(any())).thenThrow(exception);

        notificationService.sendMovieNotification("Movie");

        verify(subscriptionService).unsubscribe("dead-token");
    }

    @Test
    void sendMovieNotificationKeepsSubscriptionForTransientError() throws Exception {
        Subscription subscription = new Subscription("temporary-token");
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);

        when(subscriptionService.getAllSubscriptions()).thenReturn(List.of(subscription));
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNAVAILABLE);
        when(firebaseMessaging.send(any())).thenThrow(exception);

        notificationService.sendMovieNotification("Movie");

        verify(subscriptionService, never()).unsubscribe("temporary-token");
    }

    @Test
    void sendMovieNotificationRemovesSubscriptionWhenInvalidArgumentMentionsToken() throws Exception {
        Subscription subscription = new Subscription("broken-token");
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);

        when(subscriptionService.getAllSubscriptions()).thenReturn(List.of(subscription));
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INVALID_ARGUMENT);
        when(exception.getMessage()).thenReturn("The registration token is not a valid FCM registration token");
        when(firebaseMessaging.send(any())).thenThrow(exception);

        notificationService.sendMovieNotification("Movie");

        verify(subscriptionService).unsubscribe("broken-token");
    }
}

