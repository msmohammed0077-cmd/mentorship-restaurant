package com.mentorship.restaurant.customer;

import com.mentorship.restaurant.support.PaymentMethodEndpointTestSupport;
import org.junit.jupiter.api.Test;

class ViewPaymentMethodsEndpointTest extends PaymentMethodEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "view.payment.methods.test.";
  }

  @Test
  void listsTheDefaultFirstThenNewest() {
    long customerId = createCustomer("Sara");
    long first = addPaymentMethod(customerId, "1111");
    long second = addPaymentMethod(customerId, "2222");
    long third = addPaymentMethod(customerId, "3333");

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
    long customerId = createCustomer("Sara");

    listPaymentMethods(customerId).expectStatus().isOk().expectBody().json("[]");
  }
}
