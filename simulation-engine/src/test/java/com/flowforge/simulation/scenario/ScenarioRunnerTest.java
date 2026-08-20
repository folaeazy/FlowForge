package com.flowforge.simulation.scenario;

import com.flowforge.simulation.client.FlowForgeApiClient;
import com.flowforge.simulation.profile.SteadyLoadProfile;
import com.flowforge.simulation.report.SimulationReport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScenarioRunnerTest {

    //@Test
    void shouldSubmitJobsAccordingToProfile() {
        FlowForgeApiClient mockClient = mock(FlowForgeApiClient.class);
        when(mockClient.submitJob(anyString(), anyString(), anyMap(), anyBoolean()))
                .thenReturn(FlowForgeApiClient.SubmitResult.ACCEPTED);

        SteadyLoadProfile profile = new SteadyLoadProfile(5, 3); // 5 jobs/sec for 3 seconds = ~15 jobs
        ScenarioRunner runner = new ScenarioRunner(mockClient, profile);

        SimulationReport report = runner.run(0.0);

        // With 5 jobs/sec for 3 seconds, we expect ~15 total submissions (allowing for timing variance)
        assertThat(report.totalSubmitted()).isGreaterThanOrEqualTo(10);
        assertThat(report.totalSubmitted()).isLessThanOrEqualTo(20);
        assertThat(report.accepted()).isEqualTo(report.totalSubmitted());
    }

    //@Test
    void shouldTackRateLimitedResponses() {
        FlowForgeApiClient mockClient = mock(FlowForgeApiClient.class);
        when(mockClient.submitJob(anyString(), anyString(), anyMap(), anyBoolean()))
                .thenReturn(FlowForgeApiClient.SubmitResult.RATE_LIMITED);

        SteadyLoadProfile profile = new SteadyLoadProfile(10, 2);
        ScenarioRunner runner = new ScenarioRunner(mockClient, profile);

        SimulationReport report = runner.run(0.0);

        assertThat(report.accepted()).isZero();
        assertThat(report.rateLimited()).isGreaterThan(0);
    }
}
