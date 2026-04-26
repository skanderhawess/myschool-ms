package com.myschool.discoveryservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Disabled in CI - starts a real Eureka server which adds 6+ seconds and pollutes logs.
 * Can be enabled locally for integration testing.
 */
@Disabled("Disabled in CI - starts Eureka server")
class DiscoveryServiceApplicationTests {

    @Test
    void contextLoads() {
        // Test placeholder - starts a Eureka server
    }

}
