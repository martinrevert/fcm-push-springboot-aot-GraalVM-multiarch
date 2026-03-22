package com.example.movienotifier.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.FirebaseMessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final SubscriptionService subscriptionService;
    private final FirebaseMessaging firebaseMessaging;

    @Autowired
    public NotificationService(SubscriptionService subscriptionService, FirebaseMessaging firebaseMessaging) {
        this.subscriptionService = subscriptionService;
        this.firebaseMessaging = firebaseMessaging;
    }

    public void sendMovieNotification(String title) {
        List<Subscription> subscriptions = subscriptionService.getAllSubscriptions();
        if (subscriptions.isEmpty()) {
            logger.info("No subscribers found to send notification for movie: {}", title);
            return;
        }

        logger.info("Sending notification for movie '{}' to {} subscribers.", title, subscriptions.size());

        for (Subscription subscription : subscriptions) {
            String token = validateToken(subscription.getRegistrationToken());
            if (token == null) {
                logger.warn("Skipping subscription id={} due to blank registration token.", subscription.getId());
                continue;
            }

            logger.debug("Preparing FCM message for subscription id={} token={}",
                    subscription.getId(), shortenToken(token));

            Message message = Message.builder()
                    .putData("title", title)
                    .setToken(token)
                    .build();

            try {
                String response = firebaseMessaging.send(message);
                logger.info("Successfully sent message to token {}: {}", shortenToken(token), response);
            } catch (FirebaseMessagingException e) {
                logger.error("Failed to send message to token {}: {}", shortenToken(token), e.getMessage());
                if (shouldDeleteSubscription(e)) {
                    subscriptionService.unsubscribe(token);
                    logger.warn("Removed invalid subscription token after FCM rejection: {}", shortenToken(token));
                }
            }
        }
    }

    private String validateToken(String token) {
        if (token == null) {
            return null;
        }
        // Keep token unchanged to preserve exact bytes received from the client.
        return token.isBlank() ? null : token;
    }

    private String shortenToken(String token) {
        if (token.length() <= 12) {
            return token;
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }

    private boolean shouldDeleteSubscription(FirebaseMessagingException exception) {
        MessagingErrorCode messagingErrorCode = exception.getMessagingErrorCode();
        if (messagingErrorCode == MessagingErrorCode.UNREGISTERED || messagingErrorCode == MessagingErrorCode.SENDER_ID_MISMATCH) {
            return true;
        }

        if (messagingErrorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            return looksLikeInvalidToken(exception);
        }

        var errorCode = exception.getErrorCode();
        if (errorCode == null) {
            return false;
        }

        String normalized = errorCode.name().toUpperCase(Locale.ROOT).replace('-', '_');
        return normalized.equals("UNREGISTERED") || normalized.equals("REGISTRATION_TOKEN_NOT_REGISTERED");
    }

    private boolean looksLikeInvalidToken(FirebaseMessagingException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }

        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("registration token")
                || normalized.contains("not a valid fcm registration token")
                || normalized.contains("requested entity was not found");
    }

}
