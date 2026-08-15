package com.flowforge.simulation.report;

import java.time.Duration;

/**
 * Summary of a completed simulation scenario.
 * Printed to stdout and optionally written to a file(later).
 */
public record SimulationReport(
        String scenarioName,
        Duration actualDuration,
        int totalSubmitted,
        int accepted,
        int rateLimited,
        int queueFull,
        int errors,
        int peakRateObserved

) {
    public void print() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("SIMULATION REPORT");
        System.out.println("=".repeat(70));
        System.out.printf("Scenario: %s%n", scenarioName);
        System.out.printf("Duration: %d seconds%n", actualDuration.getSeconds());
        System.out.printf("Total submitted: %d jobs%n", totalSubmitted);
        System.out.printf("  Accepted:      %6d (%.1f%%)%n", accepted, percent(accepted, totalSubmitted));
        System.out.printf("  Rate limited:  %6d (%.1f%%)%n", rateLimited, percent(rateLimited, totalSubmitted));
        System.out.printf("  Queue full:    %6d (%.1f%%)%n", queueFull, percent(queueFull, totalSubmitted));
        System.out.printf("  Errors:        %6d (%.1f%%)%n", errors, percent(errors, totalSubmitted));
        System.out.printf("Peak rate observed: %d jobs/sec%n", peakRateObserved);
        System.out.println("=".repeat(70) + "\n");
    }

    private double percent(int part, int total) {
        return total == 0 ? 0 : (100.0 * part / total);
    }
}
