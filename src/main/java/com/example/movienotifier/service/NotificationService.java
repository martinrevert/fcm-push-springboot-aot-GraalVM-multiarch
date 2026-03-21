package com.example.movienotifier.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.FirebaseMessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final SubscriptionService subscriptionService;

    @Autowired
    public NotificationService(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    public void sendMovieNotification(String title) {
        List<Subscription> subscriptions = subscriptionService.getAllSubscriptions();
        if (subscriptions.isEmpty()) {
            logger.info("No subscribers found to send notification for movie: {}", title);
            return;
        }

        logger.info("Sending notification for movie '{}' to {} subscribers.", title, subscriptions.size());

        for (Subscription subscription : subscriptions) {
            Message message = Message.builder()
                    .putData("title", title)
                    .setToken(subscription.getRegistrationToken())
                    .build();
            try {
                String response = FirebaseMessaging.getInstance().send(message);
                logger.info("Successfully sent message to token {}: {}", subscription.getRegistrationToken(), response);
            } catch (FirebaseMessagingException e) {
                logger.error("Failed to send message to token {}: {}", subscription.getRegistrationToken(), e.getMessage());
                // Optionally, remove invalid tokens from the database
                // if (e.getErrorCode().equals("UNREGISTERED") || e.getErrorCode().equals("INVALID_ARGUMENT")) {
                //     subscriptionService.unsubscribe(subscription.getRegistrationToken());
                //     logger.warn("Removed invalid subscription token: {}", subscription.getRegistrationToken());
                // }
            }
        }
    }

}
