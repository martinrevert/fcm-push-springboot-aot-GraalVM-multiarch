package ar.com.martinrevert.movienotifier.controller;

import ar.com.martinrevert.movienotifier.model.Subscription;
import ar.com.martinrevert.movienotifier.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * Creates the subscription controller.
     *
     * @param subscriptionService subscription service
     */
    @Autowired
    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * Registers a device token for movie push notifications.
     *
     * @param token FCM registration token
     * @return created or existing subscription
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Subscription> subscribe(@RequestParam String token) {
        Subscription subscription = subscriptionService.subscribe(token);
        return ResponseEntity.ok(subscription);
    }

    /**
     * Removes a device token from movie push notifications.
     *
     * @param token FCM registration token
     * @return no-content response
     */
    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestParam String token) {
        subscriptionService.unsubscribe(token);
        return ResponseEntity.noContent().build();
    }
}



