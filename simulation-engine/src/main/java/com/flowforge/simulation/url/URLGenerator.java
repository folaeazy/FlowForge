package com.flowforge.simulation.url;


import java.util.Random;

/**
 * Generates URLs with weighted distribution:
 * - 80% fresh unique URLs (never seen before in this run)
 * - 15% pulled from the shared pool (creates duplicates)
 * - 5% malformed URLs (trigger validation failures)
 *
 * This distribution is applied independently to every request,
 * regardless of identity category.
 */
public class URLGenerator {

    private final UrlPool urlPool;
    private final Random random;
    private int freshUrlCounter = 0;


    public URLGenerator(UrlPool urlPool) {
        this.urlPool = urlPool;
        this.random = new Random(System.nanoTime()); // Different seed per generator instance
    }
}
