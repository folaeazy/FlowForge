package com.flowforge.simulation.url;


import java.util.Random;
import java.util.UUID;

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

    /**
     * Generates a URL based on the 70/20/10 weighted distribution.
     */
    public String generateUrl() {
        int roll = random.nextInt(100);

        if(roll < 70) {
            return generateFreshUrl();
        } else if (roll < 90) {
            return generateDuplicateUrl();
        }else {
           return  generateMalformedUrl();
        }
    }


    /**
     * Pick url from UrlPool 20%

     */
    private String generateDuplicateUrl() {
        String poolUrl = urlPool.getRandomFromPool();
        return poolUrl != null ? poolUrl : generateFreshUrl();
    }

    /**
     * Generate freshUrl 70% of request
     */
    private String generateFreshUrl() {
        freshUrlCounter++;
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return "https://fresh-" + freshUrlCounter + "-" + uuid + ".com/page/" + UUID.randomUUID();
    }

    /**
     * Generates a malformed URL (10% of requests, trigger validation failures).
     * Randomly picks one of three malformation types:
     * - 50% missing protocol
     * - 25% invalid characters
     * - 15% empty/whitespace
     * - 5% SQL injection type url
     */
    private String generateMalformedUrl() {
        int malformType = random.nextInt(100);

        if (malformType < 50) {
            return generateMissingProtocol();
        } else if (malformType < 75) {
            return generateInvalidCharacters();
        } else if (malformType < 85) {
            return generateEmptyOrWhitespace();
        } else {
            return generateSqlInjection();  // New
        }
    }

    private String generateMissingProtocol() {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return "example-" + uuid + ".com/page";
    }

    private String generateInvalidCharacters() {
        String[] invalidChars = {"<", ">", "{", "}", "|", "\\", "^", "`", " "};
        String base = "https://example.com/page/";
        String invalid = invalidChars[random.nextInt(invalidChars.length)];
        return base + "invalid" + invalid + "url";
    }

    private String generateEmptyOrWhitespace() {
        int choice = random.nextInt(3);
        return switch (choice) {
            case 0 -> "";
            case 1 -> "   ";  // Just spaces
            case 2 -> "\t\n"; // Tab and newline
            default -> "";
        };
    }

    private String generateSqlInjection() {
        String[] payloads = {
                "https://example.com/page'; DROP TABLE urls; --",
                "https://example.com/page' OR '1'='1",
                "https://example.com/page\" UNION SELECT * FROM users --",
                "https://example.com/page` OR 1=1 --"
        };
        return payloads[random.nextInt(payloads.length)];
    }
}
