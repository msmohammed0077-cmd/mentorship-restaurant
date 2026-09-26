package com.mentorship.restaurant.customer;

import com.mentorship.restaurant.support.PaymentMethodEndpointTestSupport;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.client.RestTestClient;

class ViewPaymentMethodsEndpointTest extends PaymentMethodEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "view.payment.methods.test.";
  }

  @Test
  void listsTheDefaultFirstThenNewest() {
    long customerId = insertCustomer(email("sara"));
    OffsetDateTime now = OffsetDateTime.now();
    long first = insertPaymentMethod(customerId, "1111", true, now.minusMinutes(3));
    long second = insertPaymentMethod(customerId, "2222", false, now.minusMinutes(2));
    long third = insertPaymentMethod(customerId, "3333", false, now.minusMinutes(1));

    listPaymentMethods(customerId)
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.length()")
        .isEqualTo(3)
        .jsonPath("$[0].payment_method_id")
        .isEqualTo(first)
        .jsonPath("$[0].is_default")
        .isEqualTo(true)
        .jsonPath("$[1].payment_method_id")
        .isEqualTo(third)
        .jsonPath("$[2].payment_method_id")
        .isEqualTo(second);
  }

  @Test
  void returnsAnEmptyListForACustomerWithNone() {
    long customerId = insertCustomer(email("sara"));

    listPaymentMethods(customerId).expectStatus().isOk().expectBody().json("[]");
  }

  @Test
  void rejectsADeletedCustomer() {
    long customerId = insertCustomer(email("sara"));
    insertPaymentMethod(customerId, "1111", true);
    softDeleteCustomer(customerId);

    listPaymentMethods(customerId)
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Customer not found");
  }

  private RestTestClient.ResponseSpec listPaymentMethods(long customerId) {
    return client
        .get()
        .uri("/api/v1/payment-methods?customerId={customerId}", customerId)
        .exchange();
  }
}
