package com.mentorship.restaurant.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.CustomerEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class ChangePasswordEndpointTest extends CustomerEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "change.password.test.";
  }

  @Test
  void changesThePasswordWhenTheCurrentOneMatches() {
    long customerId = insertCustomer(email("sara"));

    changePassword(customerId, TEST_PASSWORD, "n3w-secret-pass").expectStatus().isNoContent();

    String storedPassword =
        jdbcTemplate.queryForObject(
            "SELECT user_password FROM users WHERE user_email = ?", String.class, email("sara"));
    assertThat(passwordEncoder.matches("n3w-secret-pass", storedPassword)).isTrue();
  }

  @Test
  void rejectsAWrongCurrentPassword() {
    long customerId = insertCustomer(email("sara"));

    changePassword(customerId, "wrong-pass", "n3w-secret-pass")
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Current password is incorrect");
  }

  private RestTestClient.ResponseSpec changePassword(
      long customerId, String currentPassword, String newPassword) {
    return client
        .put()
        .uri("/api/v1/customers/{customerId}/password", customerId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "current_password": "%s",
              "new_password": "%s"
            }
            """
                .formatted(currentPassword, newPassword))
        .exchange();
  }
}
