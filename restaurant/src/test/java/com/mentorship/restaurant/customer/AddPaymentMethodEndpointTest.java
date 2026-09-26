package com.mentorship.restaurant.customer;

import com.mentorship.restaurant.support.PaymentMethodEndpointTestSupport;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class AddPaymentMethodEndpointTest extends PaymentMethodEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "add.payment.method.test.";
  }

  @Test
  void firstPaymentMethodBecomesTheDefault() {
    long customerId = insertCustomer(email("sara"));

    postPaymentMethod(customerId, paymentMethodBody("4242", VALID_EXPIRY))
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.payment_method_id")
        .isNotEmpty()
        .jsonPath("$.brand")
        .isEqualTo("VISA")
        .jsonPath("$.last4")
        .isEqualTo("4242")
        .jsonPath("$.expiry_month")
        .isEqualTo(VALID_EXPIRY.getMonthValue())
        .jsonPath("$.expiry_year")
        .isEqualTo(VALID_EXPIRY.getYear())
        .jsonPath("$.holder_name")
        .isEqualTo("Sara Youssef")
        .jsonPath("$.is_default")
        .isEqualTo(true);
  }

  @Test
  void secondPaymentMethodIsNotTheDefault() {
    long customerId = insertCustomer(email("sara"));
    insertPaymentMethod(customerId, "4242", true);

    postPaymentMethod(customerId, paymentMethodBody("1111", VALID_EXPIRY))
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.is_default")
        .isEqualTo(false);
  }

  @Test
  void rejectsAnExpiredCard() {
    long customerId = insertCustomer(email("sara"));

    postPaymentMethod(customerId, paymentMethodBody("4242", EXPIRED))
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Payment method has expired");
  }

  @Test
  void rejectsAnInvalidBody() {
    long customerId = insertCustomer(email("sara"));

    postPaymentMethod(customerId, paymentMethodBody("12a4", VALID_EXPIRY))
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void rejectsAnUnknownCustomer() {
    postPaymentMethod(999999L, paymentMethodBody("4242", VALID_EXPIRY))
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Customer not found");
  }

  private String paymentMethodBody(String last4, YearMonth expiry) {
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

  private RestTestClient.ResponseSpec postPaymentMethod(long customerId, String body) {
    return client
        .post()
        .uri("/api/v1/payment-methods?customerId={customerId}", customerId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange();
  }
}
