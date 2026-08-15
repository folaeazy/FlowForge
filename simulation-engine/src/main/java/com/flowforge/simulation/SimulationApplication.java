package com.flowforge.simulation;


import com.flowforge.simulation.client.FlowForgeApiClient;
import com.flowforge.simulation.profile.*;
import com.flowforge.simulation.report.SimulationReport;
import com.flowforge.simulation.scenario.ScenarioRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;


/**
 * Standalone load-testing application for FlowForge.
 *
 * Run scenarios interactively: responds to command-line args or runs
 * a default scenario.
 *
 * Examples:
 *   java -jar simulation-engine.jar steady 50 300      # 50 jobs/sec for 300s
 *   java -jar simulation-engine.jar burst 10 500 30 10 120
 *   java -jar simulation-engine.jar ramp 10 200 120
 *   java -jar simulation-engine.jar chaos 50 20 120 0.1
 */

@SpringBootApplication
public class SimulationApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                SpringApplication.run(SimulationApplication.class,args);
        FlowForgeApiClient client = context.getBean(FlowForgeApiClient.class);

        //Determine profile to run based on command-line args
        LoadProfile profile = parseProfile(args);
        ScenarioRunner runner = new ScenarioRunner(client, profile);
        SimulationReport report = runner.run(0.0);

        report.print();
        context.close();


    }

    private static LoadProfile parseProfile(String[] args) {
        if(args.length == 0) {
            //Default : steady 50 jobs/sec for 60 seconds
            return new  SteadyLoadProfile(50, 60);
        }

        String profileType = args[0];
        return switch (profileType) {
            case "steady" -> {
                int rate = args.length > 1 ? Integer.parseInt(args[1]) : 50;
                int duration = args.length > 2 ? Integer.parseInt(args[2]) : 60;
                yield new SteadyLoadProfile(rate, duration);
            }

            case "burst" -> {
                int baseline = args.length > 1 ? Integer.parseInt(args[1]) : 10;
                int peak = args.length > 2 ? Integer.parseInt(args[2]) : 500;
                int baselineDur = args.length > 3 ? Integer.parseInt(args[3]) : 30;
                int burstDur = args.length > 4 ? Integer.parseInt(args[4]) : 10;
                int totalDur = args.length > 5 ? Integer.parseInt(args[5]) : 120;
                yield new BurstLoadProfile(baseline, peak, baselineDur, burstDur, totalDur);
            }
            case "ramp" -> {
                int min = args.length > 1 ? Integer.parseInt(args[1]) : 10;
                int max = args.length > 2 ? Integer.parseInt(args[2]) : 200;
                int duration = args.length > 3 ? Integer.parseInt(args[3]) : 120;
                yield new RampingLoadProfile(min, max, duration);
            }

            case "chaos" -> {
                int mean = args.length > 1 ? Integer.parseInt(args[1]) : 50;
                int jitter = args.length > 2 ? Integer.parseInt(args[2]) : 20;
                int duration = args.length > 3 ? Integer.parseInt(args[3]) : 120;
                double failure = args.length > 4 ? Double.parseDouble(args[4]) : 0.0;
                yield new ChaosLoadProfile(mean, jitter, duration, failure);
            }

            default -> {
                System.err.println("Unknown profile: " + profileType);
                System.err.println("Supported: steady, burst, ramp, chaos");
                yield new SteadyLoadProfile(50, 60);
            }
        };
    }
}
