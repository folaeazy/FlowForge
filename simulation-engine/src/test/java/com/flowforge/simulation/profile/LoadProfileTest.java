package com.flowforge.simulation.profile;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LoadProfileTest {

    @Test
    void steadyProfileReturnsConstantRate() {
        SteadyLoadProfile profile = new SteadyLoadProfile(50, 10);

        assertThat(profile.targetRateAt(Duration.ofSeconds(0))).isEqualTo(50);
        assertThat(profile.targetRateAt(Duration.ofSeconds(5))).isEqualTo(50);
        assertThat(profile.targetRateAt(Duration.ofSeconds(10))).isEqualTo(0);
    }

    @Test
    void rampingProfileInterpolates() {
        RampingLoadProfile profile = new RampingLoadProfile(10, 100, 100);

        assertThat(profile.targetRateAt(Duration.ofSeconds(0))).isEqualTo(10);
        assertThat(profile.targetRateAt(Duration.ofSeconds(50))).isGreaterThan(50).isLessThan(60);
        assertThat(profile.targetRateAt(Duration.ofSeconds(100))).isEqualTo(0);
    }

    @Test
    void burstProfileSpikes() {
        BurstLoadProfile profile = new BurstLoadProfile(10, 500, 30, 10, 120);

        assertThat(profile.targetRateAt(Duration.ofSeconds(0))).isEqualTo(10);
        assertThat(profile.targetRateAt(Duration.ofSeconds(35))).isEqualTo(500); // in the burst
        assertThat(profile.targetRateAt(Duration.ofSeconds(50))).isEqualTo(10); // back to baseline
    }
}
