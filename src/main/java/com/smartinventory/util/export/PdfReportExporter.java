package com.smartinventory.util.export;

import com.smartinventory.model.report.ReportData;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure Java PDF generator creating valid, standards-compliant PDF-1.4 documents
 * with formatted headers, data tables, zebra striping, and page numbers without external libraries.
 */
public class PdfReportExporter {

    private static final float PAGE_WIDTH = 612f;  // Standard Letter
    private static final float PAGE_HEIGHT = 792f;
    private static final float MARGIN = 36f;
    private static final float USABLE_WIDTH = PAGE_WIDTH - (2 * MARGIN);
    private static final int ROWS_PER_PAGE = 26;

    public static void exportToFile(File file, ReportData report) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            new PdfBuilder(bos, report).build();
        }
    }

    private static class PdfBuilder {
        private final BufferedOutputStream out;
        private final ReportData report;
        private final List<Long> objOffsets = new ArrayList<>();
        private long bytesWritten = 0;

        public PdfBuilder(BufferedOutputStream out, ReportData report) {
            this.out = out;
            this.report = report;
        }

        public void build() throws IOException {
            writeBytes("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n");

            // Split rows across pages
            List<List<Object>> allRows = report.getRows();
            int totalPages = Math.max(1, (int) Math.ceil((double) allRows.size() / ROWS_PER_PAGE));

            // We will have:
            // Obj 1: Catalog
            // Obj 2: Pages
            // For each page:
            //   Page Obj, Content Stream Obj
            // Followed by:
            // Font1 Obj (Helvetica), Font2 Obj (Helvetica-Bold)

            int catalogObj = 1;
            int pagesObj = 2;
            int firstPageObj = 3;
            int fontRegularObj = firstPageObj + (totalPages * 2);
            int fontBoldObj = fontRegularObj + 1;

            // 1. Catalog
            startObj(catalogObj);
            writeBytes("<< /Type /Catalog /Pages " + pagesObj + " 0 R >>\nendobj\n");

            // 2. Pages
            StringBuilder kids = new StringBuilder("[");
            for (int p = 0; p < totalPages; p++) {
                int pageNumObj = firstPageObj + (p * 2);
                kids.append(pageNumObj).append(" 0 R ");
            }
            kids.append("]");

            startObj(pagesObj);
            writeBytes("<< /Type /Pages /Kids " + kids + " /Count " + totalPages + " >>\nendobj\n");

            // Generate Pages and Content Streams
            for (int p = 0; p < totalPages; p++) {
                int pageObj = firstPageObj + (p * 2);
                int contentObj = pageObj + 1;
                int startRow = p * ROWS_PER_PAGE;
                int endRow = Math.min(startRow + ROWS_PER_PAGE, allRows.size());
                List<List<Object>> pageRows = (startRow < allRows.size()) ? allRows.subList(startRow, endRow) : new ArrayList<>();

                // Page Object
                startObj(pageObj);
                writeBytes("<< /Type /Page /Parent " + pagesObj + " 0 R\n" +
                        "/MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT + "]\n" +
                        "/Contents " + contentObj + " 0 R\n" +
                        "/Resources <<\n" +
                        "/Font << /F1 " + fontRegularObj + " 0 R /F2 " + fontBoldObj + " 0 R >>\n" +
                        ">> >>\nendobj\n");

                // Content Stream Object
                byte[] contentBytes = buildPageStream(p + 1, totalPages, pageRows);
                startObj(contentObj);
                writeBytes("<< /Length " + contentBytes.length + " >>\nstream\n");
                out.write(contentBytes);
                bytesWritten += contentBytes.length;
                writeBytes("\nendstream\nendobj\n");
            }

            // Fonts
            startObj(fontRegularObj);
            writeBytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

            startObj(fontBoldObj);
            writeBytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n");

            // Cross-Reference Table
            long xrefOffset = bytesWritten;
            int totalObjs = fontBoldObj;
            writeBytes("xref\n0 " + (totalObjs + 1) + "\n0000000000 65535 f \n");
            for (int i = 1; i <= totalObjs; i++) {
                long offset = objOffsets.get(i - 1);
                writeBytes(String.format("%010d 00000 n \n", offset));
            }

            // Trailer
            writeBytes("trailer\n<< /Size " + (totalObjs + 1) + " /Root " + catalogObj + " 0 R >>\n");
            writeBytes("startxref\n" + xrefOffset + "\n%%EOF\n");
            out.flush();
        }

        private byte[] buildPageStream(int pageNumber, int totalPages, List<List<Object>> rows) {
            StringBuilder sb = new StringBuilder();

            // Background top header banner
            sb.append("0.14 0.38 0.92 rg\n"); // #2563EB Blue
            sb.append(MARGIN).append(" ").append(PAGE_HEIGHT - 65).append(" ").append(USABLE_WIDTH).append(" 36 re f\n");

            // Header Banner Text
            sb.append("BT\n");
            sb.append("/F2 14 Tf\n");
            sb.append("1 1 1 rg\n"); // White
            sb.append(MARGIN + 12).append(" ").append(PAGE_HEIGHT - 50).append(" Td\n");
            sb.append("(").append(escapePdf(report.getTitle())).append(") Tj\n");
            sb.append("ET\n");

            // Subtitle & Metadata
            sb.append("BT\n");
            sb.append("/F1 9 Tf\n");
            sb.append("0.4 0.45 0.5 rg\n");
            sb.append(MARGIN).append(" ").append(PAGE_HEIGHT - 85).append(" Td\n");
            sb.append("(Generated: ").append(escapePdf(report.getGeneratedAt()))
                    .append("   |   Operator: ").append(escapePdf(report.getGeneratedBy()))
                    .append("   |   System: Smart Inventory & Expiry Management) Tj\n");
            sb.append("ET\n");

            // Table Grid Setup
            float startY = PAGE_HEIGHT - 110;
            float rowHeight = 20f;
            List<String> headers = report.getHeaders();
            int colCount = Math.max(1, headers.size());
            float colWidth = USABLE_WIDTH / colCount;

            // Draw Table Header Row
            sb.append("0.94 0.96 0.98 rg\n"); // Light Slate
            sb.append(MARGIN).append(" ").append(startY - rowHeight).append(" ").append(USABLE_WIDTH).append(" ").append(rowHeight).append(" re f\n");

            // Header Text
            for (int c = 0; c < colCount; c++) {
                sb.append("BT\n/F2 9 Tf\n0.15 0.2 0.3 rg\n");
                float x = MARGIN + (c * colWidth) + 6;
                float y = startY - rowHeight + 6;
                sb.append(x).append(" ").append(y).append(" Td\n");
                String header = (c < headers.size()) ? headers.get(c) : "";
                sb.append("(").append(escapePdf(header)).append(") Tj\nET\n");
            }

            // Draw Header Bottom Line
            sb.append("0.8 0.85 0.9 RG\n0.75 w\n");
            sb.append(MARGIN).append(" ").append(startY - rowHeight).append(" m ")
                    .append(MARGIN + USABLE_WIDTH).append(" ").append(startY - rowHeight).append(" l S\n");

            // Draw Data Rows
            float curY = startY - rowHeight;
            for (int r = 0; r < rows.size(); r++) {
                List<Object> rowData = rows.get(r);
                curY -= rowHeight;

                // Alternating zebra stripe
                if (r % 2 == 1) {
                    sb.append("0.98 0.99 1.0 rg\n");
                    sb.append(MARGIN).append(" ").append(curY).append(" ").append(USABLE_WIDTH).append(" ").append(rowHeight).append(" re f\n");
                }

                // Row bottom border
                sb.append("0.9 0.92 0.95 RG\n0.5 w\n");
                sb.append(MARGIN).append(" ").append(curY).append(" m ")
                        .append(MARGIN + USABLE_WIDTH).append(" ").append(curY).append(" l S\n");

                // Cell Text
                for (int c = 0; c < colCount; c++) {
                    sb.append("BT\n/F1 8 Tf\n0.2 0.25 0.3 rg\n");
                    float x = MARGIN + (c * colWidth) + 6;
                    float y = curY + 6;
                    sb.append(x).append(" ").append(y).append(" Td\n");
                    String val = (c < rowData.size() && rowData.get(c) != null) ? rowData.get(c).toString() : "-";
                    // Clip cell text if too long
                    if (val.length() > 22) {
                        val = val.substring(0, 20) + "..";
                    }
                    sb.append("(").append(escapePdf(val)).append(") Tj\nET\n");
                }
            }

            // Page Footer: Page Number & Confidentiality Note
            sb.append("0.85 0.88 0.92 RG\n0.5 w\n");
            sb.append(MARGIN).append(" 30 m ").append(MARGIN + USABLE_WIDTH).append(" 30 l S\n");

            sb.append("BT\n/F1 8 Tf\n0.5 0.55 0.6 rg\n");
            sb.append(MARGIN).append(" 18 Td\n");
            sb.append("(Confidential - Internal Inventory Audit Document) Tj\n");
            sb.append("ET\n");

            sb.append("BT\n/F2 8 Tf\n0.4 0.45 0.5 rg\n");
            sb.append(MARGIN + USABLE_WIDTH - 65).append(" 18 Td\n");
            sb.append("(Page ").append(pageNumber).append(" of ").append(totalPages).append(") Tj\n");
            sb.append("ET\n");

            return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
        }

        private void startObj(int objNum) throws IOException {
            while (objOffsets.size() < objNum) {
                objOffsets.add(0L);
            }
            objOffsets.set(objNum - 1, bytesWritten);
            writeBytes(objNum + " 0 obj\n");
        }

        private void writeBytes(String s) throws IOException {
            byte[] b = s.getBytes(StandardCharsets.US_ASCII);
            out.write(b);
            bytesWritten += b.length;
        }

        private String escapePdf(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replace("\r", " ")
                    .replace("\n", " ");
        }
    }
}
