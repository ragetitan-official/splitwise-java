package com.splitwise.service;

/**
 * Interface for objects that can generate reports in multiple formats.
 */
public interface Reportable {

    /**
     * Generates a human-readable text report.
     *
     * @return the formatted text report
     */
    String generateTextReport();

    /**
     * Generates a JSON-formatted report string.
     * Implementations should build JSON manually without external libraries.
     *
     * @return the JSON report string
     */
    String generateJsonReport();
}
