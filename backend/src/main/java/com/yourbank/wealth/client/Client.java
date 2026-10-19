package com.yourbank.wealth.client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "client")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_number", nullable = false, unique = true, length = 20)
    private String clientNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 200)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(nullable = false, length = 2)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ClientCategory category;

    /** Keycloak username of the relationship manager who looks after this client. */
    @Column(name = "rm_username", nullable = false, length = 100)
    private String rmUsername;

    /** Keycloak username the client signs in with, if they use the portal. */
    @Column(name = "portal_username", unique = true, length = 100)
    private String portalUsername;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) // set by the database
    private Instant createdAt;

    protected Client() {
        // for JPA
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public Long getId() {
        return id;
    }

    public String getClientNumber() {
        return clientNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getCountry() {
        return country;
    }

    public ClientCategory getCategory() {
        return category;
    }

    public String getRmUsername() {
        return rmUsername;
    }

    public String getPortalUsername() {
        return portalUsername;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
