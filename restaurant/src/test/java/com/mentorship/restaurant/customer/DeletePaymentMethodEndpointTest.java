package com.mentorship.restaurant.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.PaymentMethodEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.client.RestTestClient;

class DeletePaymentMethodEndpointTest extends PaymentMethodEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "delete.payment.method.test.";
  }

  @Test
  void deletesThePaymentMethod() {
    long customerId = insertCustomer(email("sara"));
    long paymentMethodId = insertPaymentMethod(customerId, "1111", true);

    deletePaymentMethod(customerId, paymentMethodId).expectStatus().isNoContent();

    assertThat(paymentMethodCountFor(customerId)).isZero();
  }

  @Test
  void rejectsAnotherCustomersPaymentMethod() {
    long owner = insertCustomer(email("sara"));
    long other = insertCustomer(email("omar"));
    long paymentMethodId = insertPaymentMethod(owner, "1111", true);

    deletePaymentMethod(other, paymentMethodId)
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Payment method belongs to another customer");

    assertThat(paymentMethodCountFor(owner)).isEqualTo(1);
  }

  private RestTestClient.ResponseSpec deletePaymentMethod(long customerId, long paymentMethodId) {
    return client
        .delete()
        .uri(
            "/api/v1/payment-methods/{paymentMethodId}?customerId={customerId}",
            paymentMethodId,
            customerId)
        .exchange();
  }
}
