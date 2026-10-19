package com.yourbank.wealth.mifid;

import com.yourbank.wealth.client.Client;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** One suitability assessment of a client: a review in progress, or a completed profile. */
@Entity
@Table(name = "mifid_profile")
public class MifidProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false, updatable = false)
    private Client client;

    @Column(name = "questionnaire_version", nullable = false, length = 20)
    private String questionnaireVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private MifidStatus status;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "complex_products_ok", nullable = false)
    private boolean complexProductsOk;

    @Column(name = "completion_percent", nullable = false)
    private Integer completionPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_decision", length = 16)
    private ClientDecision clientDecision;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "signature_png")
    private String signaturePng;

    @Column(name = "created_by", nullable = false, length = 100, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** question id -> chosen option id */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mifid_answer", joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyColumn(name = "question_id", length = 40)
    @Column(name = "option_id", nullable = false, length = 40)
    private Map<String, String> answers = new HashMap<>();

    protected MifidProfile() {
        // for JPA
    }

    public static MifidProfile startReview(Client client, String rm, Instant now) {
        MifidProfile p = new MifidProfile();
        p.client = client;
        p.questionnaireVersion = MifidQuestionnaire.VERSION;
        p.status = MifidStatus.IN_PROGRESS;
        p.completionPercent = 0;
        p.createdBy = rm;
        p.createdAt = now;
        p.updatedAt = now;
        return p;
    }

    public boolean isOpenReview() {
        return status == MifidStatus.IN_PROGRESS || status == MifidStatus.PENDING_VALIDATION;
    }

    public boolean isExpired(LocalDate today) {
        return status == MifidStatus.VALID && validUntil != null && validUntil.isBefore(today);
    }

    void updateAnswers(Map<String, String> newAnswers, int completion, Instant now) {
        answers.putAll(newAnswers);
        completionPercent = completion;
        // Editing a review that was waiting for the client re-opens it.
        if (status == MifidStatus.PENDING_VALIDATION) {
            status = MifidStatus.IN_PROGRESS;
            clientDecision = null;
        }
        updatedAt = now;
    }

    void complete(MifidQuestionnaire.Score score, ClientDecision decision, String signature,
                  LocalDate validUntil, Instant now) {
        this.riskScore = score.riskScore();
        this.complexProductsOk = score.complexProductsOk();
        this.completionPercent = 100;
        this.clientDecision = decision;
        this.updatedAt = now;
        switch (decision) {
            case ACCEPTED -> {
                this.status = MifidStatus.VALID;
                this.validUntil = validUntil;
                this.signaturePng = signature;
                this.completedAt = now;
            }
            case UNDER_REVIEW -> this.status = MifidStatus.PENDING_VALIDATION;
            case REFUSED -> {
                this.status = MifidStatus.REFUSED;
                this.completedAt = now;
            }
        }
    }

    void supersede(Instant now) {
        status = MifidStatus.SUPERSEDED;
        updatedAt = now;
    }

    void cancel(Instant now) {
        status = MifidStatus.CANCELLED;
        updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public String getQuestionnaireVersion() {
        return questionnaireVersion;
    }

    public MifidStatus getStatus() {
        return status;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public boolean isComplexProductsOk() {
        return complexProductsOk;
    }

    public Integer getCompletionPercent() {
        return completionPercent;
    }

    public ClientDecision getClientDecision() {
        return clientDecision;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public String getSignaturePng() {
        return signaturePng;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Map<String, String> getAnswers() {
        return answers;
    }
}
