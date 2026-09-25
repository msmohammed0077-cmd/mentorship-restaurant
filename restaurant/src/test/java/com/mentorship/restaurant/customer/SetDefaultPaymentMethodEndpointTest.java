package com.mentorship.restaurant.customer;

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
    long customerId = createCustomer("Sara");
    long oldDefault = addPaymentMethod(customerId, "1111");
    long newDefault = addPaymentMethod(customerId, "2222");

    setDefault(customerId, newDefault)
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.payment_method_id")
        .isEqualTo(newDefault)
        .jsonPath("$.is_default")
        .isEqualTo(true);

    listPaymentMethods(customerId)
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$[0].payment_method_id")
        .isEqualTo(newDefault)
        .jsonPath("$[0].is_default")
        .isEqualTo(true)
        .jsonPath("$[1].payment_method_id")
        .isEqualTo(oldDefault)
        .jsonPath("$[1].is_default")
        .isEqualTo(false);
  }

  @Test
  void rejectsAnotherCustomersPaymentMethod() {
    long owner = createCustomer("Sara");
    long other = createCustomer("Omar");
    long paymentMethodId = addPaymentMethod(owner, "1111");

    setDefault(other, paymentMethodId)
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Payment method belongs to another customer");
  }

  @Test
  void rejectsAnUnknownPaymentMethod() {
    long customerId = createCustomer("Sara");

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
