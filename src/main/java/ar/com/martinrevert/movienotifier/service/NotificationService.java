package ar.com.martinrevert.movienotifier.service;

import ar.com.martinrevert.movienotifier.model.Subscription;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
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

    /**
     * Creates the notification service with subscription access and Firebase messaging client.
     *
     * @param subscriptionService service used to load and remove subscriptions
     * @param firebaseMessaging Firebase Admin client used to send messages
     */
    @Autowired
    public NotificationService(SubscriptionService subscriptionService, FirebaseMessaging firebaseMessaging) {
        this.subscriptionService = subscriptionService;
        this.firebaseMessaging = firebaseMessaging;
    }

    /**
     * Sends a movie notification to every currently subscribed token.
     * Invalid tokens are removed when Firebase reports terminal token errors.
     *
     * @param title movie title to include in the push payload
     */
    public void sendMovieNotification(String title) {
        String resolvedTitle = normalizeTitle(title);
        String resolvedBody = buildNotificationBody(resolvedTitle);

        List<Subscription> subscriptions = subscriptionService.getAllSubscriptions();
        if (subscriptions.isEmpty()) {
            logger.info("No subscribers found to send notification for movie: {}", resolvedTitle);
            return;
        }

        logger.info("Sending notification for movie '{}' to {} subscribers.", resolvedTitle, subscriptions.size());

        for (Subscription subscription : subscriptions) {
            String token = validateToken(subscription.getRegistrationToken());
            if (token == null) {
                logger.warn("Skipping subscription id={} due to blank registration token.", subscription.getId());
                continue;
            }

            logger.debug("Preparing FCM message for subscription id={} token={}",
                    subscription.getId(), shortenToken(token));

            Message message = buildMessage(resolvedTitle, resolvedBody, token);

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

    /**
     * Builds a platform-aware FCM message with notification and data payloads.
     *
     * @param title push notification title
     * @param body push notification body
     * @param token destination FCM registration token
     * @return fully built Firebase message
     */
    private Message buildMessage(String title, String body, String token) {
        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("title", title)
                .putData("body", body)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .putHeader("apns-priority", "10")
                        .putHeader("apns-push-type", "alert")
                        .setAps(Aps.builder()
                                .setAlert(ApsAlert.builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        .build())
                                .setSound("default")
                                .build())
                        .build())
                .build();
    }

    /**
     * Normalizes input title, returning a safe fallback when blank.
     *
     * @param title candidate movie title
     * @return normalized title used for notification payloads
     */
    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "New movie";
        }
        return title;
    }

    /**
     * Builds the notification body text displayed to the user.
     *
     * @param title normalized movie title
     * @return notification body text
     */
    private String buildNotificationBody(String title) {
        return "Now available: " + title;
    }

    /**
     * Validates token presence while preserving original token bytes.
     *
     * @param token raw registration token from storage
     * @return token if usable, otherwise null
     */
    private String validateToken(String token) {
        if (token == null) {
            return null;
        }
        // Keep token unchanged to preserve exact bytes received from the client.
        return token.isBlank() ? null : token;
    }

    /**
     * Produces a shortened token representation for safe log output.
     *
     * @param token full registration token
     * @return masked token preserving start and end segments
     */
    private String shortenToken(String token) {
        if (token.length() <= 12) {
            return token;
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }

    /**
     * Determines whether a Firebase error means the subscription must be deleted.
     *
     * @param exception Firebase send exception
     * @return true when the token is permanently invalid/unregistered
     */
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

    /**
     * Detects invalid-token signals in INVALID_ARGUMENT error messages.
     *
     * @param exception Firebase send exception
     * @return true when message text indicates an invalid registration token
     */
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

