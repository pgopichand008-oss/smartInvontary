package com.smartinventory.util.export;

import com.smartinventory.model.report.ReportData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Clean CSV export utility for standard spreadsheet interoperability.
 */
public class CsvReportExporter {

    public static void exportToFile(File file, ReportData report) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(osw)) {

            // UTF-8 BOM for Microsoft Excel auto-detection
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);

            // Title & Meta
            writer.write(escapeCsv(report.getTitle()));
            writer.newLine();
            writer.write(escapeCsv("Generated: " + report.getGeneratedAt() + " | By: " + report.getGeneratedBy()));
            writer.newLine();
            writer.newLine();

            // Headers
            List<String> headers = report.getHeaders();
            for (int i = 0; i < headers.size(); i++) {
                writer.write(escapeCsv(headers.get(i)));
                if (i < headers.size() - 1) writer.write(",");
            }
            writer.newLine();

            // Data Rows
            for (List<Object> row : report.getRows()) {
                for (int i = 0; i < row.size(); i++) {
                    Object val = row.get(i);
                    writer.write(escapeCsv(val != null ? val.toString() : ""));
                    if (i < row.size() - 1) writer.write(",");
                }
                writer.newLine();
            }

            writer.flush();
        }
    }

    private static String escapeCsv(String s) {
        if (s == null) return "\"\"";
        boolean mustQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String escaped = s.replace("\"", "\"\"");
        return mustQuote ? ("\"" + escaped + "\"") : escaped;
    }
}
