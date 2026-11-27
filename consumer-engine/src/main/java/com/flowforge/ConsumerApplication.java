package com.flowforge;

public class ConsumerApplication {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -jar consumer-engine.jar <durationSeconds> <readsPerSecond> [baseUrl]");
            System.exit(1);
        }
        int duration = Integer.parseInt(args[0]);
        int rate = Integer.parseInt(args[1]);
        String baseUrl = args.length > 2 ? args[2]
                : System.getenv().getOrDefault("FLOWFORGE_BASE_URL", "http://localhost:8080");

        new ConsumerRunner(new FlowForgeQueryClient(baseUrl)).run(duration, rate);
    }
}