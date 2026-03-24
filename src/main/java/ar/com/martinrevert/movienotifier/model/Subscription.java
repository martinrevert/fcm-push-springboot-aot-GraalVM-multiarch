package ar.com.martinrevert.movienotifier.model;

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

    /**
     * Creates an empty subscription and sets current timestamp.
     */
    public Subscription() {
        this.subscribedAt = LocalDateTime.now();
    }

    /**
     * Creates a subscription with token and current timestamp.
     *
     * @param registrationToken FCM registration token
     */
    public Subscription(String registrationToken) {
        this.registrationToken = registrationToken;
        this.subscribedAt = LocalDateTime.now();
    }

    /**
     * Gets the subscription identifier.
     *
     * @return subscription id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the subscription identifier.
     *
     * @param id subscription id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the registration token.
     *
     * @return FCM registration token
     */
    public String getRegistrationToken() {
        return registrationToken;
    }

    /**
     * Sets the registration token.
     *
     * @param registrationToken FCM registration token
     */
    public void setRegistrationToken(String registrationToken) {
        this.registrationToken = registrationToken;
    }

    /**
     * Gets subscription timestamp.
     *
     * @return subscription creation time
     */
    public LocalDateTime getSubscribedAt() {
        return subscribedAt;
    }

    /**
     * Sets subscription timestamp.
     *
     * @param subscribedAt subscription creation time
     */
    public void setSubscribedAt(LocalDateTime subscribedAt) {
        this.subscribedAt = subscribedAt;
    }

    /**
     * Returns debug-friendly subscription representation.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return "Subscription{" +
               "id=" + id +
               ", registrationToken='" + registrationToken + '\'' +
               ", subscribedAt=" + subscribedAt +
               '}';
    }
}


