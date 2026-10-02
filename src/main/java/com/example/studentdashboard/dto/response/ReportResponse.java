package com.example.studentdashboard.dto.response;

import java.util.List;

/**
 * rows are pre-formatted strings (not raw numbers) so the frontend's
 * generic report-preview table can render any of the 5 report types
 * without per-type formatting logic — matches how the mock layer already
 * builds reports.
 */
public record ReportResponse(
        String title,
        List<String> columns,
        List<List<String>> rows,
        String session,
        String term,
        String generatedAt
) {
}
