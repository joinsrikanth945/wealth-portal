package com.yourbank.wealth.instrument;

import java.math.BigDecimal;
import java.time.LocalDate;

/** An instrument as the API shows it. */
public record InstrumentDto(
        Long id,
        String code,
        String name,
        AssetClass assetClass,
        String currency,
        String exchange,
        int riskClass,
        boolean complex,
        BigDecimal lastPrice,
        LocalDate priceDate,
        Long underlyingId,
        String optionType,
        BigDecimal strike,
        LocalDate expiry,
        BigDecimal contractSize) {

    public static InstrumentDto from(Instrument i) {
        return new InstrumentDto(
                i.getId(), i.getCode(), i.getName(), i.getAssetClass(), i.getCurrency(), i.getExchange(),
                i.getRiskClass(), i.isComplex(), i.getLastPrice(), i.getPriceDate(),
                i.getUnderlying() == null ? null : i.getUnderlying().getId(),
                i.getOptionType(), i.getStrike(), i.getExpiry(), i.getContractSize());
    }
}
