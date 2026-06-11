package com.flowforge.core.domain;

/**
 * Represents life cycle of a
 * DEAD job doesn't enter the main queue - moved to DLQ
 */
public enum JobStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    DEAD

}
