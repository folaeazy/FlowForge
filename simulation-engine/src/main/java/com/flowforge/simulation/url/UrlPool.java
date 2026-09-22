package com.flowforge.simulation.url;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Holds a shared pool of 300 base URLs that any identity can randomly
 * pull from to create realistic duplicates across the simulation.
 * Any identity requesting the same URL should get the same short code back (global idempotency).
 */
public class UrlPool {
    private final List<String> pool;
    private final Random random;

    public UrlPool() {
        this(300);
    }

    public UrlPool(int poolSize){
        this.pool = new ArrayList<>();
        this.random = new Random(42);

        initializePool(poolSize);
    }

    private void initializePool(int poolSize) {

        String[] domains = {
                "example.com", "techblog.io", "news.org", "tutorial.dev",
                "docs.api.com", "learning.edu", "opensource.io", "github.com",
                "stackoverflow.com", "medium.com", "dev.to", "product.hunt"
        };

        String[] paths = {
                "/article", "/guide", "/post", "/page", "/docs", "/tutorial",
                "/howto", "/learn", "/blog", "/video", "/resource", "/tool"
        };

        for(int i = 0; i < poolSize; i++) {
            String domain = domains[random.nextInt(domains.length)];
            String path = paths[random.nextInt(paths.length)];
            String id = String.valueOf(i + 1);

            String url = "https://" + domain + path + "/" + id;
            pool.add(url);
        }
    }


    /**
     * Returns a random URL from the shared pool (creates a duplicate).
     */
    public String getRandomFromPool() {
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(random.nextInt(pool.size()));
    }

    /**
     * Adds a new URL to the pool .
     */
    public void addNew(String url) {
        if (url != null && !url.isBlank()) {
            pool.add(url);
        }
    }

    public int size() {
        return pool.size();
    }

    public List<String> getAllUrls() {
        return new ArrayList<>(pool);
    }

}
