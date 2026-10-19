package com.yourbank.wealth.messaging;

import com.yourbank.wealth.client.Client;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** One secure message in the conversation between a client and their RM. */
@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false, updatable = false)
    private Client client;

    @Column(name = "from_client", nullable = false, updatable = false)
    private boolean fromClient;

    @Column(nullable = false, length = 100, updatable = false)
    private String sender;

    @Column(name = "sender_name", nullable = false, length = 200, updatable = false)
    private String senderName;

    @Column(nullable = false, length = 4000, updatable = false)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected Message() {
        // for JPA
    }

    public Message(Client client, boolean fromClient, String sender, String senderName, String body, Instant now) {
        this.client = client;
        this.fromClient = fromClient;
        this.sender = sender;
        this.senderName = senderName;
        this.body = body;
        this.createdAt = now;
    }

    void markRead(Instant now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    public Long getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public boolean isFromClient() {
        return fromClient;
    }

    public String getSender() {
        return sender;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
