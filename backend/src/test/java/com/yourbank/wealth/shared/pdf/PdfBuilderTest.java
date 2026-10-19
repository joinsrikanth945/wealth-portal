package com.yourbank.wealth.shared.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PdfBuilderTest {

    @Test
    void buildsAMultiPagePdfWithAccentsAndUnsupportedCharacters() {
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            rows.add(new String[] {"Amélie García " + i, "EUR 1,234.56", "€ — ✓ 漢"});
        }

        byte[] pdf = new PdfBuilder("Client statement", "Amélie Laurent · P-2006")
                .heading("Holdings")
                .keyValues(List.<String[]>of(new String[] {"Total value", "EUR 1,000.00"}))
                .paragraph("A long paragraph ".repeat(60))
                .table(new String[] {"Name", "Value", "Symbols"}, new float[] {0.5f, 0.25f, 0.25f},
                        new boolean[] {false, true, false}, rows)
                .total("Total", "EUR 1,000.00")
                .signature("Signed", "data:image/png;base64,not-really-a-png")
                .toBytes();

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(2000);
    }
}
