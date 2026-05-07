import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rate.limiter.TenantRateLimiterService;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TenantRateLimiterServiceTest {



    private  TenantRateLimiterService tenant;

    @BeforeEach
    void setUp() {
         tenant = new TenantRateLimiterService(100,1000);
    }


    // should create bucket per tenant - no duplicate bucket/tenant
    @Test
    @DisplayName("Bucket per tenant")
    void shouldCreateBucketPerTenant() {

        tenant.tryAcquire("tenant-A");
        tenant.tryAcquire("tenant-B");
        tenant.tryAcquire("tenant-A");

        assertEquals(2, tenant.activeTenantCount());


    }

    // should create bucket on first access
    @Test
    @DisplayName("Create bucket on first access")
    void shouldCreateBucketOnFirstAccess() {
        boolean result = tenant.tryAcquire("A");

        assertTrue(result);
        assertEquals(1, tenant.activeTenantCount());


    }

    // register tenant with custom limit
    @Test
    @DisplayName("custom reg")
    void shouldRegisterTenantWithCustomLimit() {
        tenant.tryAcquire("premium");
        tenant.registerTenant("premium", 200, 1000);
        long tokens = tenant.availableTokens("premium");
        assertEquals(200, tokens);

    }
}
