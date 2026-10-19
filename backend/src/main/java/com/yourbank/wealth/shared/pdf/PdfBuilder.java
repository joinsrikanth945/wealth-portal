package com.yourbank.wealth.shared.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * A small helper for the bank's PDF documents (statements, reports, proposals):
 * a header band, headings, key/value blocks, paragraphs, tables and a signature
 * image, with automatic page breaks and page numbers.
 *
 * <p>Usage: {@code new PdfBuilder("Title", "Subtitle").heading("...").table(...).toBytes()}
 */
public class PdfBuilder {

    private static final float MARGIN = 48;
    private static final float WIDTH = PDRectangle.A4.getWidth() - 2 * MARGIN;
    private static final float TOP = PDRectangle.A4.getHeight() - MARGIN;
    private static final float BOTTOM = MARGIN + 28;

    private static final float[] INK = {0.07f, 0.13f, 0.18f};
    private static final float[] MUTED = {0.33f, 0.41f, 0.48f};
    private static final float[] ACCENT = {0.05f, 0.37f, 0.36f};
    private static final float[] LINE = {0.86f, 0.89f, 0.91f};

    private final PDDocument doc = new PDDocument();
    private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final List<PDPage> pages = new ArrayList<>();
    private final String footer;
    private PDPageContentStream cs;
    private float y;

    public PdfBuilder(String title, String subtitle) {
        this(title, subtitle, "[Your Bank] Wealth - demo document. Prices are sample values, not market data.");
    }

    public PdfBuilder(String title, String subtitle, String footer) {
        this.footer = footer;
        newPage();
        try {
            // Header band
            fill(ACCENT);
            cs.addRect(0, PDRectangle.A4.getHeight() - 8, PDRectangle.A4.getWidth(), 8);
            cs.fill();
            text("[Your Bank] Wealth", MARGIN, y, bold, 10, ACCENT);
            y -= 26;
            text(title, MARGIN, y, bold, 20, INK);
            y -= 18;
            if (subtitle != null) {
                text(subtitle, MARGIN, y, regular, 10, MUTED);
                y -= 10;
            }
            y -= 14;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public PdfBuilder heading(String heading) {
        ensureSpace(40);
        y -= 8;
        write(heading, MARGIN, bold, 12, INK);
        y -= 6;
        rule();
        y -= 12;
        return this;
    }

    /** Label / value pairs in two columns. */
    public PdfBuilder keyValues(List<String[]> pairs) {
        for (String[] kv : pairs) {
            ensureSpace(16);
            write(kv[0], MARGIN, regular, 10, MUTED);
            float x = MARGIN + 170;
            write(kv[1], x, bold, 10, INK);
            y -= 15;
        }
        y -= 4;
        return this;
    }

    public PdfBuilder paragraph(String text) {
        if (text == null || text.isBlank()) {
            return this;
        }
        for (String line : wrap(safe(text, regular), regular, 10, WIDTH)) {
            ensureSpace(15);
            write(line, MARGIN, regular, 10, INK);
            y -= 14;
        }
        y -= 4;
        return this;
    }

    /**
     * A table. {@code widths} are fractions of the page width (they should add up to 1);
     * {@code right[i]} right-aligns column i (use it for numbers).
     */
    public PdfBuilder table(String[] headers, float[] widths, boolean[] right, List<String[]> rows) {
        ensureSpace(40);
        drawRow(headers, widths, right, bold, MUTED);
        y -= 4;
        rule();
        y -= 13;
        if (rows.isEmpty()) {
            write("Nothing to show.", MARGIN, regular, 9, MUTED);
            y -= 15;
        }
        for (String[] row : rows) {
            if (y < BOTTOM + 16) {
                newPage();
                drawRow(headers, widths, right, bold, MUTED);
                y -= 4;
                rule();
                y -= 13;
            }
            drawRow(row, widths, right, regular, INK);
            y -= 15;
        }
        y -= 6;
        return this;
    }

    /** A total line under a table. */
    public PdfBuilder total(String label, String value) {
        ensureSpace(20);
        rule();
        y -= 14;
        write(label, MARGIN, bold, 10, INK);
        float w = width(value, bold, 10);
        write(value, MARGIN + WIDTH - w, bold, 10, INK);
        y -= 18;
        return this;
    }

    /** Embeds a PNG given as a data URL (the client's signature). */
    public PdfBuilder signature(String label, String dataUrl) {
        if (dataUrl == null || !dataUrl.contains(",")) {
            return this;
        }
        try {
            byte[] png = Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1));
            PDImageXObject image = PDImageXObject.createFromByteArray(doc, png, "signature");
            float w = 220;
            float h = w * image.getHeight() / Math.max(1, image.getWidth());
            ensureSpace(h + 30);
            write(label, MARGIN, regular, 10, MUTED);
            y -= h + 6;
            cs.drawImage(image, MARGIN, y, w, h);
            y -= 12;
        } catch (IOException | IllegalArgumentException e) {
            write(label + ": signature on file", MARGIN, regular, 10, MUTED);
            y -= 15;
        }
        return this;
    }

    public byte[] toBytes() {
        try {
            cs.close();
            int total = pages.size();
            for (int i = 0; i < total; i++) {
                try (PDPageContentStream footerStream =
                             new PDPageContentStream(doc, pages.get(i), PDPageContentStream.AppendMode.APPEND, true)) {
                    PDPageContentStream saved = cs;
                    cs = footerStream;
                    text(footer, MARGIN, MARGIN, regular, 8, MUTED);
                    String pageNo = "Page " + (i + 1) + " of " + total;
                    text(pageNo, MARGIN + WIDTH - width(pageNo, regular, 8), MARGIN, regular, 8, MUTED);
                    cs = saved;
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            doc.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ------------------------------------------------------------------ internals

    private void drawRow(String[] cells, float[] widths, boolean[] right, PDType1Font font, float[] color) {
        float x = MARGIN;
        for (int i = 0; i < cells.length; i++) {
            float colWidth = widths[i] * WIDTH;
            String cell = fit(safe(cells[i] == null ? "" : cells[i], font), font, 9, colWidth - 6);
            float cx = right != null && right[i] ? x + colWidth - width(cell, font, 9) - 4 : x;
            write(cell, cx, font, 9, color);
            x += colWidth;
        }
    }

    private void newPage() {
        try {
            if (cs != null) {
                cs.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            pages.add(page);
            cs = new PDPageContentStream(doc, page);
            y = TOP;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void ensureSpace(float needed) {
        if (y - needed < BOTTOM) {
            newPage();
        }
    }

    private void rule() {
        try {
            stroke(LINE);
            cs.setLineWidth(0.7f);
            cs.moveTo(MARGIN, y);
            cs.lineTo(MARGIN + WIDTH, y);
            cs.stroke();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void write(String s, float x, PDType1Font font, float size, float[] color) {
        try {
            text(safe(s, font), x, y, font, size, color);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void text(String s, float x, float atY, PDType1Font font, float size, float[] color) throws IOException {
        fill(color);
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, atY);
        cs.showText(safe(s, font));
        cs.endText();
    }

    private void fill(float[] c) throws IOException {
        cs.setNonStrokingColor(c[0], c[1], c[2]);
    }

    private void stroke(float[] c) throws IOException {
        cs.setStrokingColor(c[0], c[1], c[2]);
    }

    private float width(String s, PDType1Font font, float size) {
        try {
            return font.getStringWidth(safe(s, font)) / 1000 * size;
        } catch (IOException e) {
            return s.length() * size * 0.5f;
        }
    }

    /** Replaces characters the standard PDF fonts can't show (they only cover Western European text). */
    static String safe(String s, PDType1Font font) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            try {
                font.encode(ch);
                out.append(ch);
            } catch (IOException | IllegalArgumentException e) {
                out.append('?');
            }
            i += Character.charCount(cp);
        }
        return out.toString();
    }

    /** Cuts text that would not fit its column, adding "..." */
    private String fit(String s, PDType1Font font, float size, float max) {
        if (width(s, font, size) <= max) {
            return s;
        }
        String cut = s;
        while (cut.length() > 1 && width(cut + "...", font, size) > max) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    private List<String> wrap(String text, PDType1Font font, float size, float max) {
        List<String> lines = new ArrayList<>();
        for (String para : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : para.split(" ")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (width(candidate, font, size) > max && !line.isEmpty()) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }
}
