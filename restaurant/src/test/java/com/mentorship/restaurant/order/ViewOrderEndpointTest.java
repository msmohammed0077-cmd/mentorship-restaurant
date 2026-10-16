package com.mentorship.restaurant.order;

import com.mentorship.restaurant.support.OrderEndpointTestSupport;
import org.junit.jupiter.api.Test;

/**
 * Pins view-order as it behaves today, ahead of the #84 refactor. Assertions tagged "#82" record
 * behaviour that is known to be wrong.
 */
class ViewOrderEndpointTest extends OrderEndpointTestSupport {

  private static final String ORDER = "/api/v1/orders/{id}";

  @Test
  void returnsTheOrderWithItsLines() {
    long orderId = seedOrder("PLACED");
    seedOrderLine(orderId, KOFTA, 2);

    asCustomer(CUSTOMER)
        .get()
        .uri(ORDER, orderId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(orderId)
        .jsonPath("$.status")
        .isEqualTo("PLACED")
        .jsonPath("$.customer_id")
        .isEqualTo(CUSTOMER)
        .jsonPath("$.restaurant_id")
        .isEqualTo(NILE_KITCHEN)
        .jsonPath("$.address_id")
        .exists()
        .jsonPath("$.total")
        .isEqualTo(370.00)
        .jsonPath("$.order_items.length()")
        .isEqualTo(1)
        .jsonPath("$.order_items[0].menu_item_id")
        .isEqualTo(KOFTA)
        .jsonPath("$.order_items[0].quantity")
        .isEqualTo(2)
        .jsonPath("$.order_items[0].total_price")
        .isEqualTo(370.00);
  }

  @Test
  void omitsTheTransactionForACashOrder() {
    long orderId = seedOrder("PLACED");

    asCustomer(CUSTOMER)
        .get()
        .uri(ORDER, orderId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.transaction_response")
        .isEmpty();
  }

  @Test
  void letsTheRestaurantReadItsOrder() {
    long orderId = seedOrder("PLACED");

    asRestaurantOwner(NILE_KITCHEN).get().uri(ORDER, orderId).exchange().expectStatus().isOk();
  }

  @Test
  void hidesTheOrderFromAnotherCustomer() {
    long orderId = seedOrder("PLACED");

    asCustomer(2L).get().uri(ORDER, orderId).exchange().expectStatus().isNotFound();
  }

  @Test
  void hidesTheOrderFromAnotherRestaurant() {
    long orderId = seedOrder("PLACED");

    asRestaurantOwner(BURGER_YARD).get().uri(ORDER, orderId).exchange().expectStatus().isNotFound();
  }

  @Test
  void refusesAnUnknownOrder() {
    asCustomer(CUSTOMER).get().uri(ORDER, 999999L).exchange().expectStatus().isNotFound();
  }
}
