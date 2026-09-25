package com.mentorship.restaurant.customer;

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
    long customerId = createCustomer("Sara");
    long paymentMethodId = addPaymentMethod(customerId, "1111");

    deletePaymentMethod(customerId, paymentMethodId).expectStatus().isNoContent();

    listPaymentMethods(customerId).expectStatus().isOk().expectBody().json("[]");
  }

  @Test
  void rejectsAnotherCustomersPaymentMethod() {
    long owner = createCustomer("Sara");
    long other = createCustomer("Omar");
    long paymentMethodId = addPaymentMethod(owner, "1111");

    deletePaymentMethod(other, paymentMethodId)
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Payment method belongs to another customer");

    listPaymentMethods(owner)
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.length()")
        .isEqualTo(1);
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
