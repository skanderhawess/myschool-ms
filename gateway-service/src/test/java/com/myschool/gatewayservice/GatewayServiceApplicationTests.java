package com.myschool.gatewayservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Disabled in CI - requires Eureka server (discovery-service) to be running.
 * Can be enabled locally when the full microservices stack is up.
 */
@Disabled("Disabled in CI - requires Eureka server")
class GatewayServiceApplicationTests {

    @Test
    void contextLoads() {
        // Test placeholder - requires Eureka discovery service
    }

}
