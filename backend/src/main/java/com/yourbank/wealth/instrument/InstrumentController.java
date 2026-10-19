package com.yourbank.wealth.instrument;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/instruments")
@Tag(name = "Instruments", description = "Security master: shares, bonds, funds, ETFs and derivatives")
public class InstrumentController {

    private final InstrumentRepository instruments;

    public InstrumentController(InstrumentRepository instruments) {
        this.instruments = instruments;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Search instruments by name or code, optionally by asset class")
    public List<InstrumentDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) AssetClass assetClass) {
        String text = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return instruments.findAllByOrderByNameAsc().stream()
                .filter(i -> assetClass == null || i.getAssetClass() == assetClass)
                .filter(i -> text.isEmpty()
                        || i.getName().toLowerCase(Locale.ROOT).contains(text)
                        || i.getCode().toLowerCase(Locale.ROOT).contains(text))
                .limit(50)
                .map(InstrumentDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "One instrument")
    public InstrumentDto get(@PathVariable Long id) {
        return instruments.findById(id)
                .map(InstrumentDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));
    }
}
