package com.flowforge.core.ports;

import java.util.List;

/**
 *  Defines what metrics the system tracks
 */
public interface MetricsStore {


    long getQueueSize();
    /**
     * Returns per-minute (success, failed) counts for the last N minutes,
     * oldest first — directly maps to the dashboard's Job Throughput chart.
     */
    List<TpsDataPoint> getTpsRange(int lastNMinutes);

    record TpsDataPoint(String minuteLabel, long success , long failed) {}

}
