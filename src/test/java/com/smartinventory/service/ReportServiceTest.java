package com.smartinventory.service;

import com.smartinventory.model.enums.TransactionType;
import com.smartinventory.model.report.ReportData;
import com.smartinventory.model.report.ReportType;
import com.smartinventory.util.export.CsvReportExporter;
import com.smartinventory.util.export.ExcelReportExporter;
import com.smartinventory.util.export.PdfReportExporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated test suite for Phase 7 Reporting Services and pure-Java PDF/Excel/CSV Exporters.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReportServiceTest {

    private static ReportService reportService;

    @BeforeAll
    public static void setUp() {
        reportService = new ReportService();
    }

    @Test
    @Order(1)
    @DisplayName("Report Service: Generate Inventory Valuation Report")
    public void testGenerateInventoryValuationReport() {
        ReportData report = reportService.generateInventoryValuationReport(null, "TestAdmin");
        assertNotNull(report);
        assertEquals(ReportType.INVENTORY_VALUATION, report.getReportType());
        assertEquals("TestAdmin", report.getGeneratedBy());
        assertNotNull(report.getHeaders());
        assertTrue(report.getHeaders().contains("Batch #"));
        assertTrue(report.getHeaders().contains("Total Value ($)"));
        assertNotNull(report.getRows());
        assertNotNull(report.getSummaryNotes());
        assertFalse(report.getSummaryNotes().isEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("Report Service: Generate Expiry Surveillance Report")
    public void testGenerateExpirySurveillanceReport() {
        ReportData report = reportService.generateExpirySurveillanceReport(null, 30, "Auditor");
        assertNotNull(report);
        assertEquals(ReportType.EXPIRY_SURVEILLANCE, report.getReportType());
        assertEquals("Auditor", report.getGeneratedBy());
        assertTrue(report.getHeaders().contains("Timeline"));
        assertTrue(report.getHeaders().contains("Severity"));
        assertNotNull(report.getRows());
        assertNotNull(report.getSummaryNotes());
    }

    @Test
    @Order(3)
    @DisplayName("Report Service: Generate Low-Stock & Reorder Deficit Report")
    public void testGenerateLowStockReport() {
        ReportData report = reportService.generateLowStockReport(null, "ProcurementLead");
        assertNotNull(report);
        assertEquals(ReportType.LOW_STOCK_REORDER, report.getReportType());
        assertTrue(report.getHeaders().contains("Safety Level"));
        assertTrue(report.getHeaders().contains("Deficit"));
        assertNotNull(report.getRows());
        assertNotNull(report.getSummaryNotes());
    }

    @Test
    @Order(4)
    @DisplayName("Report Service: Generate Stock Movement Audit Ledger")
    public void testGenerateTransactionAuditReport() {
        ReportData report = reportService.generateTransactionAuditReport(null, TransactionType.PURCHASE_IN, 25, "SystemAuditor");
        assertNotNull(report);
        assertEquals(ReportType.TRANSACTION_AUDIT, report.getReportType());
        assertTrue(report.getHeaders().contains("Movement Type"));
        assertTrue(report.getHeaders().contains("Balance After"));
        assertNotNull(report.getRows());
        assertNotNull(report.getSummaryNotes());
    }

    @Test
    @Order(5)
    @DisplayName("PDF Exporter: Build valid PDF-1.4 file with pure Java")
    public void testPdfReportExporter() throws IOException {
        ReportData report = createMockReport("Mock PDF Inventory Report");
        File tempPdf = File.createTempFile("test_report_", ".pdf");
        tempPdf.deleteOnExit();

        PdfReportExporter.exportToFile(tempPdf, report);

        assertTrue(tempPdf.exists(), "PDF file must exist");
        assertTrue(tempPdf.length() > 0, "PDF file must not be empty");

        byte[] bytes = Files.readAllBytes(tempPdf.toPath());
        String content = new String(bytes, 0, Math.min(bytes.length, 100));
        assertTrue(content.startsWith("%PDF-1.4"), "PDF must begin with valid header %PDF-1.4");

        String tail = new String(bytes, Math.max(0, bytes.length - 200), Math.min(bytes.length, 200));
        assertTrue(tail.contains("%%EOF"), "PDF must terminate with %%EOF marker");
    }

    @Test
    @Order(6)
    @DisplayName("Excel Exporter: Build valid SpreadsheetML (.xls) file with pure Java")
    public void testExcelReportExporter() throws IOException {
        ReportData report = createMockReport("Mock Excel Inventory Report");
        File tempXls = File.createTempFile("test_report_", ".xls");
        tempXls.deleteOnExit();

        ExcelReportExporter.exportToFile(tempXls, report);

        assertTrue(tempXls.exists(), "Excel file must exist");
        assertTrue(tempXls.length() > 0, "Excel file must not be empty");

        String xml = Files.readString(tempXls.toPath());
        assertTrue(xml.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"), "Must be valid XML");
        assertTrue(xml.contains("xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\""), "Must contain SpreadsheetML namespace");
        assertTrue(xml.contains("<Worksheet ss:Name="), "Must contain worksheet element");
        assertTrue(xml.contains("Mock Excel Inventory Report"), "Must contain report title");
        assertTrue(xml.contains("SKU-001"), "Must contain cell data");
    }

    @Test
    @Order(7)
    @DisplayName("CSV Exporter: Build valid CSV file with UTF-8 BOM")
    public void testCsvReportExporter() throws IOException {
        ReportData report = createMockReport("Mock CSV Inventory Report");
        File tempCsv = File.createTempFile("test_report_", ".csv");
        tempCsv.deleteOnExit();

        CsvReportExporter.exportToFile(tempCsv, report);

        assertTrue(tempCsv.exists(), "CSV file must exist");
        assertTrue(tempCsv.length() > 0, "CSV file must not be empty");

        byte[] bytes = Files.readAllBytes(tempCsv.toPath());
        // Verify UTF-8 BOM (0xEF, 0xBB, 0xBF)
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);

        String text = new String(bytes, 3, bytes.length - 3);
        assertTrue(text.contains("Mock CSV Inventory Report"));
        assertTrue(text.contains("Item Name,SKU,In Stock"));
        assertTrue(text.contains("Widget A,SKU-001,100"));
    }

    private ReportData createMockReport(String title) {
        List<String> headers = Arrays.asList("Item Name", "SKU", "In Stock", "Price");
        List<List<Object>> rows = Arrays.asList(
                Arrays.asList("Widget A", "SKU-001", 100, "$10.50"),
                Arrays.asList("Gadget B", "SKU-002", 45, "$22.00"),
                Arrays.asList("Special, Item \"C\"", "SKU-003", 10, "$5.75")
        );
        List<String> notes = Arrays.asList("Total items: 3", "Status: Verified");

        return new ReportData(ReportType.INVENTORY_VALUATION, title, "TestUser", headers, rows, notes);
    }
}
