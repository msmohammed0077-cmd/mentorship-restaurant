package com.mentorship.restaurant.health;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

  private final String applicationName;

  public HealthController(@Value("${spring.application.name}") String applicationName) {
    this.applicationName = applicationName;
  }

  // Public: an empty requirement overrides the global Keycloak one in the OpenAPI docs.
  @SecurityRequirements
  @GetMapping("/health")
  public HealthResponse health() {
    return new HealthResponse("UP", applicationName);
  }
}
