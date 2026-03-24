package com.example.movienotifier.controller;

import com.example.movienotifier.model.Subscription;
import com.example.movienotifier.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Subscription> subscribe(@RequestParam String token) {
        Subscription subscription = subscriptionService.subscribe(token);
        return ResponseEntity.ok(subscription);
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestParam String token) {
        subscriptionService.unsubscribe(token);
        return ResponseEntity.noContent().build();
    }
}


