package com.flowforge.simulation.identity;


import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.*;

/**
 * Manages a pool of 4,000 identities (IP addresses) split into three categories:
 * - 70% unique (2,800 IPs, fire 1 request each)
 * - 20% recurring (800 IPs, fire 5-10 requests each)
 * - 10% burst (400 IPs, fire 10-20 requests in tight clusters)
 **/
public class IdentityPool {
    public enum Category { UNIQUE, RECURRING, BURST }

    private static final double UNIQUE_SHARE = 0.70;
    private static final double RECURRING_SHARE = 0.20;
    private static final double BURST_SHARE = 0.10;

    private final Random random;
    private final Set<String> issued = new HashSet<>();   // no IP is ever handed out twice as a new identity
    private final String[] recurringIps;
    private final int[] recurringRemaining;
    private final List<String> burstIps = new ArrayList<>();

    private double singleBudget = 0;
    private double burstBudget = 0;
    private int pendingBurstSize;

    public IdentityPool() { this(800, 500, 42L); }

    public IdentityPool(int recurringPoolSize, int burstPoolSize, long seed) {
        this.random = new Random(seed);
        this.recurringIps = new String[recurringPoolSize];
        this.recurringRemaining = new int[recurringPoolSize];
        for (int i = 0; i < recurringPoolSize; i++) {
            recurringIps[i] = freshIp();
            recurringRemaining[i] = rollRecurringVisits();
        }
        for (int i = 0; i < burstPoolSize; i++) {
            burstIps.add(freshIp());
        }
        this.pendingBurstSize = rollBurstSize();
    }

    /**
     * Identities for one tick, in firing order. Burst clusters stay contiguous so the runner
     * sends them back-to-back. A tick that completes a burst can exceed n; the budgets carry
     * the difference so the long-run rate and the 70/20/10 mix hold.
     */
    public List<Identity> nextBatch(int n) {
        List<List<Identity>> blocks = new ArrayList<>();

        burstBudget += n * BURST_SHARE;
        while (burstBudget >= pendingBurstSize) {
            String ip = burstIps.get(random.nextInt(burstIps.size()));
            List<Identity> cluster = new ArrayList<>(pendingBurstSize);
            for (int i = 0; i < pendingBurstSize; i++) {
                cluster.add(new Identity(ip, Category.BURST));
            }
            blocks.add(cluster);
            burstBudget -= pendingBurstSize;
            pendingBurstSize = rollBurstSize();
        }

        singleBudget += n * (1 - BURST_SHARE);
        int singles = (int) singleBudget;
        singleBudget -= singles;

        double uniqueFraction = UNIQUE_SHARE / (UNIQUE_SHARE + RECURRING_SHARE);
        for (int i = 0; i < singles; i++) {
            Identity id = random.nextDouble() < uniqueFraction
                    ? new Identity(freshIp(), Category.UNIQUE)
                    : new Identity(nextRecurringIp(), Category.RECURRING);
            blocks.add(List.of(id));
        }

        Collections.shuffle(blocks, random);
        List<Identity> batch = new ArrayList<>();
        blocks.forEach(batch::addAll);
        return batch;
    }

    private String nextRecurringIp() {
        int i = random.nextInt(recurringIps.length);
        String ip = recurringIps[i];
        if (--recurringRemaining[i] == 0) {          // this visitor has used up their visits
            recurringIps[i] = freshIp();
            recurringRemaining[i] = rollRecurringVisits();
        }
        return ip;
    }

    private int rollRecurringVisits() { return 5 + random.nextInt(6); }   // 5-10
    private int rollBurstSize()       { return 10 + random.nextInt(11); } // 10-20

    private String freshIp() {
        String ip;
        do {
            ip = randomIp();
        } while (!issued.add(ip));
        return ip;
    }

    private String randomIp() {
        int first;
        do {
            first = 1 + random.nextInt(223);
        } while (first == 10 || first == 127);
        return first + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + (1 + random.nextInt(254));
    }
}
