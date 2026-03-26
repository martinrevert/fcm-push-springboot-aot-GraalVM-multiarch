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
import java.util.stream.Collectors;

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
        sendMovieNotification(title, null, null, null, null, null);
    }

    /**
     * Sends a movie notification with enriched metadata.
     *
     * @param title movie title
     * @param movieId movie id
     * @param posterUrl poster image URL
     * @param genres movie genres
     * @param language movie language
     * @param rating movie rating
     */
    public void sendMovieNotification(String title, Integer movieId, String posterUrl, List<String> genres, String language, Double rating) {
        String resolvedTitle = normalizeTitle(title);
        String resolvedMovieId = normalizeMovieId(movieId);
        String resolvedPosterUrl = normalizePosterUrl(posterUrl);
        String resolvedGenres = normalizeGenres(genres);
        String resolvedLanguage = normalizeLanguage(language);
        String resolvedRating = normalizeRating(rating);
        String resolvedBody = buildNotificationBody(resolvedGenres, resolvedLanguage, resolvedRating);

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

            Message message = buildMessage(
                resolvedTitle,
                resolvedMovieId,
                resolvedBody,
                resolvedPosterUrl,
                resolvedGenres,
                resolvedLanguage,
                resolvedRating,
                token
            );

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
     * @param movieId movie id
     * @param body push notification body
     * @param posterUrl poster image URL
     * @param genres normalized genres text
     * @param language normalized language text
     * @param rating normalized rating text
     * @param token destination FCM registration token
     * @return fully built Firebase message
     */
    private Message buildMessage(String title, String movieId, String body, String posterUrl, String genres, String language, String rating, String token) {
        Notification.Builder notificationBuilder = Notification.builder()
                .setTitle(title)
                .setBody(body);

        AndroidNotification.Builder androidNotificationBuilder = AndroidNotification.builder()
                .setTitle(title)
                .setBody(body);

        if (posterUrl != null) {
            notificationBuilder.setImage(posterUrl);
            androidNotificationBuilder.setImage(posterUrl);
        }

        return Message.builder()
                .setToken(token)
                .setNotification(notificationBuilder.build())
                .putData("title", title)
                .putData("id", movieId)
                .putData("body", body)
                .putData("posterUrl", defaultString(posterUrl))
                .putData("genres", genres)
                .putData("language", language)
                .putData("rating", rating)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(androidNotificationBuilder.build())
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
     * Normalizes movie id into a string-safe payload value.
     *
     * @param movieId movie id candidate
     * @return normalized movie id text
     */
    private String normalizeMovieId(Integer movieId) {
        return movieId == null ? "" : movieId.toString();
    }

    /**
     * Builds the notification body text displayed to the user.
     *
     * @param genres normalized genres text
     * @param language normalized language text
     * @param rating normalized rating text
     * @return notification body text
     */
    private String buildNotificationBody(String genres, String language, String rating) {
        return "Genres: " + genres + " | Language: " + language + " | Rating: " + rating;
    }

    /**
     * Normalizes poster URL.
     *
     * @param posterUrl poster URL candidate
     * @return URL when present, otherwise null
     */
    private String normalizePosterUrl(String posterUrl) {
        if (posterUrl == null || posterUrl.isBlank()) {
            return null;
        }
        return posterUrl;
    }

    /**
     * Normalizes genres list into a comma-separated string.
     *
     * @param genres genres list
     * @return normalized genres text
     */
    private String normalizeGenres(List<String> genres) {
        if (genres == null || genres.isEmpty()) {
            return "Unknown";
        }
        String joined = genres.stream()
            .filter(value -> value != null && !value.isBlank())
            .collect(Collectors.joining(", "));
        return joined.isBlank() ? "Unknown" : joined;
    }

    /**
     * Normalizes language string.
     *
     * @param language language candidate
     * @return normalized language
     */
    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "Unknown";
        }
        return language;
    }

    /**
     * Normalizes rating value.
     *
     * @param rating rating candidate
     * @return normalized rating text
     */
    private String normalizeRating(Double rating) {
        if (rating == null) {
            return "N/A";
        }
        return String.format(Locale.US, "%.1f", rating);
    }

    /**
     * Converts nullable value to non-null string for data payload.
     *
     * @param value input value
     * @return original value or empty string
     */
    private String defaultString(String value) {
        return value == null ? "" : value;
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

