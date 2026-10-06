package com.mentorship.restaurant.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.CustomerEndpointTestSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Pins create-order as it behaves today, ahead of the #84 refactor. Assertions tagged "#82" record
 * behaviour that is known to be wrong; the PR that fixes it flips the assertion.
 */
class CreateOrderEndpointTest extends CustomerEndpointTestSupport {

  private static final String ORDERS = "/api/v1/orders";
  private static final long KOFTA = 1L;
  private static final long FALAFEL = 2L;
  private static final long BURGER_YARD = 2L;

  @Override
  protected String emailPrefix() {
    return "create.order.test.";
  }

  @Test
  void placesACashOrder() {
    long customerId = insertCustomer(email("sara"));
    long addressId = addressIdForCustomer(jdbcTemplate, customerId);
    long cartId = insertCart(customerId);
    insertCartItem(cartId, KOFTA, 2);
    insertCartItem(cartId, FALAFEL, 1);

    placeOrder(fieldsFor(cartId, addressId, customerId, NILE_KITCHEN, "CASH_ON_DELIVERY"))
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.id")
        .exists()
        .jsonPath("$.status")
        .isEqualTo("PLACED")
        .jsonPath("$.customer_id")
        .isEqualTo(customerId)
        .jsonPath("$.restaurant_id")
        .isEqualTo(NILE_KITCHEN)
        .jsonPath("$.address_id")
        .isEqualTo(addressId)
        .jsonPath("$.total")
        .isEqualTo(435.00)
        .jsonPath("$.order_items.length()")
        .isEqualTo(2)
        .jsonPath("$.transaction_response")
        .isEmpty();

    assertThat(orderCountFor(customerId)).isEqualTo(1);
    Integer lines =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM order_items
            WHERE order_id = (SELECT order_id FROM orders WHERE customer_id = ?)
            """,
            Integer.class,
            customerId);
    assertThat(lines).isEqualTo(2);
  }

  @Test
  void placesACardOrder() {
    long customerId = insertCustomer(email("sara"));
    long addressId = addressIdForCustomer(jdbcTemplate, customerId);
    long cartId = insertCart(customerId);
    insertCartItem(cartId, KOFTA, 1);

    Map<String, Object> fields = fieldsFor(cartId, addressId, customerId, NILE_KITCHEN, "CARD");
    fields.put("card_id", "card-123");

    placeOrder(fields)
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.transaction_response.status")
        .isEqualTo("PAID");

    long orderId =
        jdbcTemplate.queryForObject(
            "SELECT order_id FROM orders WHERE customer_id = ?", Long.class, customerId);
    assertThat(transactionFor(orderId))
        .hasValueSatisfying(row -> assertThat(row.get("status")).isEqualTo("PAID"));
  }

  private RestTestClient.ResponseSpec placeOrder(Map<String, Object> fields) {
    return client
        .post()
        .uri(ORDERS)
        .contentType(MediaType.APPLICATION_JSON)
        .body(orderBody(fields))
        .exchange();
  }

  private Map<String, Object> fieldsFor(
      long cartId, long addressId, long customerId, long restaurantId, String paymentMethod) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("cart_id", cartId);
    fields.put("address_id", addressId);
    fields.put("customer_id", customerId);
    fields.put("restaurant_id", restaurantId);
    if (paymentMethod != null) {
      fields.put("payment_method", paymentMethod);
    }
    return fields;
  }

  /** Flat JSON: numbers as-is, everything else quoted. Enough for this request. */
  private String orderBody(Map<String, Object> fields) {
    return fields.entrySet().stream()
        .map(
            e ->
                "\"%s\": %s"
                    .formatted(
                        e.getKey(),
                        e.getValue() instanceof Number ? e.getValue() : "\"" + e.getValue() + "\""))
        .collect(Collectors.joining(", ", "{", "}"));
  }
}
