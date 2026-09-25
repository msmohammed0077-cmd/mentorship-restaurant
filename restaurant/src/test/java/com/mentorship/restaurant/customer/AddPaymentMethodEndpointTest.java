package com.mentorship.restaurant.customer;

import com.mentorship.restaurant.support.PaymentMethodEndpointTestSupport;
import org.junit.jupiter.api.Test;

class AddPaymentMethodEndpointTest extends PaymentMethodEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "add.payment.method.test.";
  }

  @Test
  void firstPaymentMethodBecomesTheDefault() {
    long customerId = createCustomer("Sara");

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
    long customerId = createCustomer("Sara");
    addPaymentMethod(customerId, "4242");

    postPaymentMethod(customerId, paymentMethodBody("1111", VALID_EXPIRY))
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.is_default")
        .isEqualTo(false);
  }

  @Test
  void rejectsAnExpiredCard() {
    long customerId = createCustomer("Sara");

    postPaymentMethod(customerId, paymentMethodBody("4242", EXPIRED))
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Payment method has expired");
  }

  @Test
  void rejectsAnInvalidBody() {
    long customerId = createCustomer("Sara");

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
}
