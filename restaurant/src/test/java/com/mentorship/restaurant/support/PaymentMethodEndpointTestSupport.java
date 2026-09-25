package com.mentorship.restaurant.support;

import java.time.YearMonth;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Payment method tests use customers they create through the API, never the seeded ones. Every
 * email is under the subclass's prefix, so cleanup can only reach that class's users; their
 * customers and payment methods follow by ON DELETE CASCADE.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public abstract class PaymentMethodEndpointTestSupport {

  /** Valid through the end of its month, so "now plus two years" is always in date. */
  protected static final YearMonth VALID_EXPIRY = YearMonth.now().plusYears(2);

  protected static final YearMonth EXPIRED = YearMonth.now().minusMonths(1);

  @Autowired protected RestTestClient client;
  @Autowired protected JdbcTemplate jdbcTemplate;

  /** Unique per test class, e.g. {@code "add.payment.method.test."}. */
  protected abstract String emailPrefix();

  @BeforeEach
  @AfterEach
  void deleteCreatedUsers() {
    jdbcTemplate.update("DELETE FROM users WHERE user_email LIKE ?", emailPrefix() + "%");
  }

  protected long createCustomer(String firstName) {
    String email = emailPrefix() + firstName.toLowerCase(Locale.ROOT) + "@example.com";
    client
        .post()
        .uri("/api/v1/customers")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "name": "%s Youssef",
              "email": "%s",
              "password": "s3cret-pass"
            }
            """
                .formatted(firstName, email))
        .exchange()
        .expectStatus()
        .isCreated();
    return jdbcTemplate.queryForObject(
        """
        SELECT c.customer_id FROM customers c JOIN users u ON u.user_id = c.user_id
        WHERE u.user_email = ?
        """,
        Long.class,
        email);
  }

  protected String paymentMethodBody(String last4, YearMonth expiry) {
    return """
        {
          "brand": "VISA",
          "last4": "%s",
          "expiry_month": %d,
          "expiry_year": %d,
          "holder_name": "Sara Youssef"
        }
        """
        .formatted(last4, expiry.getMonthValue(), expiry.getYear());
  }

  protected RestTestClient.ResponseSpec postPaymentMethod(long customerId, String body) {
    return client
        .post()
        .uri("/api/v1/payment-methods?customerId={customerId}", customerId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange();
  }

  /** Adds a valid card through the API and returns its id. */
  protected long addPaymentMethod(long customerId, String last4) {
    postPaymentMethod(customerId, paymentMethodBody(last4, VALID_EXPIRY))
        .expectStatus()
        .isCreated();
    return jdbcTemplate.queryForObject(
        """
        SELECT payment_method_id FROM payment_methods
        WHERE customer_id = ? AND payment_method_last4 = ?
        """,
        Long.class,
        customerId,
        last4);
  }

  protected RestTestClient.ResponseSpec listPaymentMethods(long customerId) {
    return client
        .get()
        .uri("/api/v1/payment-methods?customerId={customerId}", customerId)
        .exchange();
  }
}
