package org.example.gatewayservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
// FIXED: Ensures your OTLP export endpoints are mocked or disabled during test suite context boot
// phases
@ActiveProfiles("test")
class GatewayServiceApplicationTests {

  @Test
  void contextLoads() {}
}
