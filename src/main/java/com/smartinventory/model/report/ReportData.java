package com.smartinventory.model.report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Universal tabular data structure representing compiled report output.
 */
public class ReportData {

    private final ReportType reportType;
    private final String title;
    private final String generatedAt;
    private final String generatedBy;
    private final List<String> headers;
    private final List<List<Object>> rows;
    private final List<String> summaryNotes;

    public ReportData(ReportType reportType, String title, String generatedBy,
                      List<String> headers, List<List<Object>> rows,
                      List<String> summaryNotes) {
        this.reportType = reportType;
        this.title = title;
        this.generatedBy = generatedBy != null ? generatedBy : "System";
        this.generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.headers = headers != null ? headers : Collections.emptyList();
        this.rows = rows != null ? rows : Collections.emptyList();
        this.summaryNotes = summaryNotes != null ? summaryNotes : Collections.emptyList();
    }

    public ReportType getReportType() {
        return reportType;
    }

    public String getTitle() {
        return title;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<List<Object>> getRows() {
        return rows;
    }

    public List<String> getSummaryNotes() {
        return summaryNotes;
    }

    public int getRowCount() {
        return rows.size();
    }
}
