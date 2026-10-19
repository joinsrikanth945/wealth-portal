package com.yourbank.wealth.instrument;

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
import java.math.BigDecimal;
import java.time.LocalDate;

/** One tradable product in the security master: share, bond, fund, ETF, option or future. */
@Entity
@Table(name = "instrument")
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 16)
    private AssetClass assetClass;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 32)
    private String exchange;

    /** 1 (lowest) to 7 (highest), as on a PRIIPs key information document. */
    @Column(name = "risk_class", nullable = false)
    private Integer riskClass;

    /** MiFID "complex" product: needs an appropriateness check before trading. */
    @Column(nullable = false)
    private boolean complex;

    @Column(name = "last_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal lastPrice;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "underlying_id")
    private Instrument underlying;

    @Column(name = "option_type", length = 4)
    private String optionType;

    @Column(precision = 19, scale = 6)
    private BigDecimal strike;

    private LocalDate expiry;

    @Column(name = "contract_size", precision = 19, scale = 6)
    private BigDecimal contractSize;

    protected Instrument() {
        // for JPA
    }

    /**
     * How much one unit of quantity is worth at a given price. Bonds are
     * quoted in % of nominal; derivatives are multiplied by the contract size.
     */
    public BigDecimal valueOf(BigDecimal quantity, BigDecimal price) {
        BigDecimal value = quantity.multiply(price);
        if (assetClass == AssetClass.BOND) {
            return value.movePointLeft(2);
        }
        if (contractSize != null) {
            return value.multiply(contractSize);
        }
        return value;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public AssetClass getAssetClass() {
        return assetClass;
    }

    public String getCurrency() {
        return currency;
    }

    public String getExchange() {
        return exchange;
    }

    public Integer getRiskClass() {
        return riskClass;
    }

    public boolean isComplex() {
        return complex;
    }

    public BigDecimal getLastPrice() {
        return lastPrice;
    }

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public Instrument getUnderlying() {
        return underlying;
    }

    public String getOptionType() {
        return optionType;
    }

    public BigDecimal getStrike() {
        return strike;
    }

    public LocalDate getExpiry() {
        return expiry;
    }

    public BigDecimal getContractSize() {
        return contractSize;
    }
}
