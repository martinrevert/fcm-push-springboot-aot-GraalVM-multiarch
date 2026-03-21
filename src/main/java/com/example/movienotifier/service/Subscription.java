package com.example.movienotifier.service;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String registrationToken;

    @Column(nullable = false)
    private LocalDateTime subscribedAt;

    // Constructors
    public Subscription() {
        this.subscribedAt = LocalDateTime.now();
    }

    public Subscription(String registrationToken) {
        this.registrationToken = registrationToken;
        this.subscribedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRegistrationToken() {
        return registrationToken;
    }

    public void setRegistrationToken(String registrationToken) {
        this.registrationToken = registrationToken;
    }

    public LocalDateTime getSubscribedAt() {
        return subscribedAt;
    }

    public void setSubscribedAt(LocalDateTime subscribedAt) {
        this.subscribedAt = subscribedAt;
    }

    @Override
    public String toString() {
        return "Subscription{" +
               "id=" + id +
               ", registrationToken='" + registrationToken + '\'' +
               ", subscribedAt=" + subscribedAt +
               '}';
    }
}
