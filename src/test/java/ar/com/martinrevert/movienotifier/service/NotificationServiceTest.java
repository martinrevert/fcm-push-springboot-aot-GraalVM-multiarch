package ar.com.martinrevert.movienotifier.service;

import ar.com.martinrevert.movienotifier.model.Subscription;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Test
    void sendMovieNotificationSkipsBlankTokens() throws Exception {
        Subscription blank = new Subscription("   ");
        blank.setId(1L);
        Subscription nullToken = new Subscription(null);
        nullToken.setId(2L);
        Subscription valid = new Subscription("valid-token");
        valid.setId(3L);

        when(subscriptionService.getAllSubscriptions()).thenReturn(List.of(blank, nullToken, valid));
        when(firebaseMessaging.send(any())).thenReturn("ok");

        notificationService.sendMovieNotification("Movie");

        verify(firebaseMessaging, times(1)).send(any());
    }

    @Test
    void sendMovieNotificationBuildsNotificationAndDataPayload() throws Exception {
        Subscription valid = new Subscription("valid-token");
        when(subscriptionService.getAllSubscriptions()).thenReturn(List.of(valid));
        when(firebaseMessaging.send(any())).thenReturn("ok");

        notificationService.sendMovieNotification(
            "Movie",
            "https://img.example/poster.jpg",
            List.of("Action", "Drama"),
            "Spanish",
            8.7
        );

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(firebaseMessaging).send(messageCaptor.capture());
        Message sentMessage = messageCaptor.getValue();
        assertEquals("valid-token", extractToken(sentMessage));
        Map<String, String> data = extractData(sentMessage);
        assertEquals("Movie", data.get("title"));
        assertEquals("Genres: Action, Drama | Language: Spanish | Rating: 8.7", data.get("body"));
        assertEquals("https://img.example/poster.jpg", data.get("posterUrl"));
        assertEquals("Action, Drama", data.get("genres"));
        assertEquals("Spanish", data.get("language"));
        assertEquals("8.7", data.get("rating"));

        Object notification = extractNotification(sentMessage);
        assertEquals("Movie", extractStringField(notification, "title"));
        assertEquals("Genres: Action, Drama | Language: Spanish | Rating: 8.7", extractStringField(notification, "body"));
        assertEquals("https://img.example/poster.jpg", extractStringField(notification, "image"));
    }

    private String extractToken(Message message) throws Exception {
        return extractStringField(message, "token");
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractData(Message message) throws Exception {
        Field field = Message.class.getDeclaredField("data");
        field.setAccessible(true);
        return (Map<String, String>) field.get(message);
    }

    private Object extractNotification(Message message) throws Exception {
        Field field = Message.class.getDeclaredField("notification");
        field.setAccessible(true);
        return field.get(message);
    }

    private String extractStringField(Object object, String fieldName) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(object);
    }
}


