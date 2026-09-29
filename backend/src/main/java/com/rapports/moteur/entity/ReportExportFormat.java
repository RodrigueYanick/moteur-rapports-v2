package com.rapports.moteur.entity;

public enum ReportExportFormat {
    PDF,
    EXCEL,
    PNG,
    JPEG,
    CSV,
    JSON;

    public static ReportExportFormat fromString(String value) {
        if (value == null || value.isBlank()) {
            return PDF;
        }
        try {
            return ReportExportFormat.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PDF;
        }
    }
}
