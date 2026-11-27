package com.flowforge;

import java.util.ArrayList;
import java.util.List;

public class LatencyStats {

    private final List<Long> micros = new ArrayList<>();

    void add(long m) { micros.add(m); }
    int count() { return micros.size(); }

    double percentileMs(double p) {
        if (micros.isEmpty()) return 0;
        List<Long> sorted = micros.stream().sorted().toList();
        int idx = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1))) / 1000.0;
    }
}
