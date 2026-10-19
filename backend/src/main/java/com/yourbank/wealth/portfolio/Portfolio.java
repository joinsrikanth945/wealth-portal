package com.yourbank.wealth.portfolio;

import com.yourbank.wealth.client.Client;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "portfolio")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_number", nullable = false, unique = true, length = 20)
    private String portfolioNumber;

    @Column(nullable = false, length = 120)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 20)
    private ServiceType serviceType;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "opened_on", nullable = false)
    private LocalDate openedOn;

    protected Portfolio() {
        // for JPA
    }

    public Long getId() {
        return id;
    }

    public String getPortfolioNumber() {
        return portfolioNumber;
    }

    public String getName() {
        return name;
    }

    public Client getClient() {
        return client;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getOpenedOn() {
        return openedOn;
    }
}
