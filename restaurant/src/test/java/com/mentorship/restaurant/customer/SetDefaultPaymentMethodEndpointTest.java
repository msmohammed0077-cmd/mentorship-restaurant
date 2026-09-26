package com.mentorship.restaurant.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.PaymentMethodEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.client.RestTestClient;

class SetDefaultPaymentMethodEndpointTest extends PaymentMethodEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "set.default.payment.method.test.";
  }

  @Test
  void switchesTheDefault() {
    long customerId = insertCustomer(email("sara"));
    long oldDefault = insertPaymentMethod(customerId, "1111", true);
    long newDefault = insertPaymentMethod(customerId, "2222", false);

    setDefault(customerId, newDefault)
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.payment_method_id")
        .isEqualTo(newDefault)
        .jsonPath("$.is_default")
        .isEqualTo(true);

    assertThat(isDefault(newDefault)).isTrue();
    assertThat(isDefault(oldDefault)).isFalse();
  }

  @Test
  void rejectsAnotherCustomersPaymentMethod() {
    long owner = insertCustomer(email("sara"));
    long other = insertCustomer(email("omar"));
    long paymentMethodId = insertPaymentMethod(owner, "1111", true);

    setDefault(other, paymentMethodId)
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Payment method belongs to another customer");
  }

  @Test
  void rejectsAnUnknownPaymentMethod() {
    long customerId = insertCustomer(email("sara"));

    setDefault(customerId, 999999L)
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Payment method not found");
  }

  private RestTestClient.ResponseSpec setDefault(long customerId, long paymentMethodId) {
    return client
        .put()
        .uri(
            "/api/v1/payment-methods/{paymentMethodId}/default?customerId={customerId}",
            paymentMethodId,
            customerId)
        .exchange();
  }
}
