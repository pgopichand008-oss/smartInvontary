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
 * Pure Java Excel generator outputting Microsoft Office SpreadsheetML (.xls),
 * fully preserving column headers, cell types, colors, and formatting without external libraries.
 */
public class ExcelReportExporter {

    public static void exportToFile(File file, ReportData report) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(osw)) {

            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<?mso-application progid=\"Excel.Sheet\"?>\n");
            writer.write("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
            writer.write(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n");
            writer.write(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n");
            writer.write(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
            writer.write(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n");

            // Styles Definition
            writer.write(" <Styles>\n");
            writer.write("  <Style ss:ID=\"Default\" ss:Name=\"Normal\">\n");
            writer.write("   <Alignment ss:Vertical=\"Center\"/>\n");
            writer.write("   <Font ss:FontName=\"Segoe UI\" ss:Size=\"10\" ss:Color=\"#1E293B\"/>\n");
            writer.write("  </Style>\n");

            writer.write("  <Style ss:ID=\"TitleStyle\">\n");
            writer.write("   <Font ss:FontName=\"Segoe UI\" ss:Bold=\"1\" ss:Size=\"15\" ss:Color=\"#1E40AF\"/>\n");
            writer.write("  </Style>\n");

            writer.write("  <Style ss:ID=\"MetaStyle\">\n");
            writer.write("   <Font ss:FontName=\"Segoe UI\" ss:Italic=\"1\" ss:Size=\"9\" ss:Color=\"#64748B\"/>\n");
            writer.write("  </Style>\n");

            writer.write("  <Style ss:ID=\"HeaderStyle\">\n");
            writer.write("   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\n");
            writer.write("   <Borders>\n");
            writer.write("    <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#93C5FD\"/>\n");
            writer.write("   </Borders>\n");
            writer.write("   <Font ss:FontName=\"Segoe UI\" ss:Bold=\"1\" ss:Size=\"10\" ss:Color=\"#FFFFFF\"/>\n");
            writer.write("   <Interior ss:Color=\"#2563EB\" ss:Pattern=\"Solid\"/>\n");
            writer.write("  </Style>\n");

            writer.write("  <Style ss:ID=\"RowEven\">\n");
            writer.write("   <Interior ss:Color=\"#FFFFFF\" ss:Pattern=\"Solid\"/>\n");
            writer.write("  </Style>\n");

            writer.write("  <Style ss:ID=\"RowOdd\">\n");
            writer.write("   <Interior ss:Color=\"#F8FAFC\" ss:Pattern=\"Solid\"/>\n");
            writer.write("  </Style>\n");

            writer.write(" </Styles>\n");

            // Worksheet
            writer.write(" <Worksheet ss:Name=\"Inventory Report\">\n");
            writer.write("  <Table ss:DefaultRowHeight=\"20\">\n");

            // Set column default widths
            for (int c = 0; c < report.getHeaders().size(); c++) {
                writer.write("   <Column ss:Width=\"130\"/>\n");
            }

            // Title Row
            writer.write("   <Row ss:Height=\"28\">\n");
            writer.write("    <Cell ss:StyleID=\"TitleStyle\"><Data ss:Type=\"String\">" + escapeXml(report.getTitle()) + "</Data></Cell>\n");
            writer.write("   </Row>\n");

            // Metadata Row
            writer.write("   <Row ss:Height=\"18\">\n");
            writer.write("    <Cell ss:StyleID=\"MetaStyle\"><Data ss:Type=\"String\">Generated: " + escapeXml(report.getGeneratedAt()) +
                    "  |  Operator: " + escapeXml(report.getGeneratedBy()) +
                    "  |  System: Smart Inventory &amp; Expiry Management</Data></Cell>\n");
            writer.write("   </Row>\n");

            // Empty spacer row
            writer.write("   <Row ss:Height=\"10\"/>\n");

            // Table Headers
            writer.write("   <Row ss:Height=\"24\">\n");
            for (String h : report.getHeaders()) {
                writer.write("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">" + escapeXml(h) + "</Data></Cell>\n");
            }
            writer.write("   </Row>\n");

            // Data Rows
            List<List<Object>> rows = report.getRows();
            for (int r = 0; r < rows.size(); r++) {
                String rowStyle = (r % 2 == 1) ? "RowOdd" : "RowEven";
                writer.write("   <Row ss:StyleID=\"" + rowStyle + "\" ss:Height=\"20\">\n");
                for (Object val : rows.get(r)) {
                    if (val == null) {
                        writer.write("    <Cell><Data ss:Type=\"String\">-</Data></Cell>\n");
                    } else if (val instanceof Number) {
                        writer.write("    <Cell><Data ss:Type=\"Number\">" + val + "</Data></Cell>\n");
                    } else {
                        writer.write("    <Cell><Data ss:Type=\"String\">" + escapeXml(val.toString()) + "</Data></Cell>\n");
                    }
                }
                writer.write("   </Row>\n");
            }

            // Summary notes
            if (!report.getSummaryNotes().isEmpty()) {
                writer.write("   <Row ss:Height=\"10\"/>\n");
                for (String note : report.getSummaryNotes()) {
                    writer.write("   <Row ss:Height=\"18\">\n");
                    writer.write("    <Cell ss:StyleID=\"MetaStyle\"><Data ss:Type=\"String\">" + escapeXml(note) + "</Data></Cell>\n");
                    writer.write("   </Row>\n");
                }
            }

            writer.write("  </Table>\n");
            writer.write(" </Worksheet>\n");
            writer.write("</Workbook>\n");

            writer.flush();
        }
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
