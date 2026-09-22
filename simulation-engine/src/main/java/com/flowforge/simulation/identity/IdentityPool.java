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

    private final List<String> allIdentities;
    private final Queue<String> uniqueQueue;
    private final Queue<String> recurringQueue;
    private final Queue<String> burstQueue;
    private final Random random;
    private final Map<String, Integer> ipFireCount;

    public IdentityPool(int totalIdentities) {
        this.allIdentities = new ArrayList<>();
        this.uniqueQueue = new ArrayDeque<>();
        this.recurringQueue = new ArrayDeque<>();
        this.burstQueue = new ArrayDeque<>();
        this.ipFireCount = new HashMap<>();
        this.random = new Random(42);

        initialize(totalIdentities);
    }


    public int totalIdentities() {
        return allIdentities.size();
    }

    /**
     * Picks the next identity (IP) to fire a request.
     */
    public String nextIdentity(Category category) {
        return switch (category) {
            case UNIQUE -> uniqueQueue.poll();
            case RECURRING -> {
                String ip = recurringQueue.poll();
                if(ip != null) recurringQueue.offer(ip); // cycle it back to the end
                yield ip;
            }
            case BURST -> {
                String ip = burstQueue.poll();
                if(ip != null) burstQueue.offer(ip); // cycle it back to the end
                yield ip;
            }
        };

    }

    /**
     * Returns the next identity (IP) by randomly selecting a category.
     * Uses weighted distribution: 70% unique, 20% recurring, 10% burst.
     */
    public String getNextIdentity() {
        int roll = random.nextInt(100);

        Category category = roll < 70 ? Category.UNIQUE
                : roll < 90 ? Category.RECURRING
                : Category.BURST;

        String ip = nextIdentity(category);
        if(ip != null) decrementFireCount(ip);
        return ip;
    }


    private void initialize(int total) {
        int uniqueCount = (int) (total * 0.70) ;
        int recurringCount = (int) (total * 0.20) ;
        int burstCount = total - uniqueCount - recurringCount;


        // create unique user Identities/IP
        for(int i = 0; i < uniqueCount; i++) {
            String ip = generateRandomIP();
            allIdentities.add(ip);
            uniqueQueue.offer(ip);
            ipFireCount.put(ip, 1); //unique fires only once
        }

        // create recurring user Identities/IP
        for(int i = 0; i < recurringCount; i++) {
            String ip = generateRandomIP();
            allIdentities.add(ip);
            recurringQueue.offer(ip);
            int fireCount = 5 + random.nextInt(6);  // 5-10
            ipFireCount.put(ip, fireCount);
        }

        // create burst user Identities/IP
        for(int i = 0; i < burstCount; i++) {
            String ip = generateRandomIP();
            allIdentities.add(ip);
            burstQueue.offer(ip);
            int fireCount = 10 + random.nextInt(11);  // 10-20
            ipFireCount.put(ip, fireCount);
        }
    }

    /**
     * Generate Random IPV4 Address
     */
    private String generateRandomIP() {
        return random.nextInt(256) + "." +
                random.nextInt(256) + "." +
                random.nextInt(256) + "." +
                random.nextInt(256) ;

    }

    public int getRemainingFires(String ip) {
        return ipFireCount.getOrDefault(ip, 0);
    }

    // Add method to decrement fires:
    public void decrementFireCount(String ip) {
        ipFireCount.put(ip, ipFireCount.getOrDefault(ip, 0) - 1);
    }

    public enum Category {
        UNIQUE,
        RECURRING,
        BURST
    }

    // A little test code
    public static void main(String[] args) throws InterruptedException{
        IdentityPool pool = new IdentityPool(10);
        System.out.println("Unique queue size " + pool.uniqueQueue.size());
        System.out.println("Recurring queue size " + pool.recurringQueue.size());
        System.out.println("Burst queue size " + pool.burstQueue.size());

        while (!pool.recurringQueue.isEmpty()) {
            System.out.println(pool.nextIdentity(Category.RECURRING));
            Thread.sleep(100);
        }
    }
}
