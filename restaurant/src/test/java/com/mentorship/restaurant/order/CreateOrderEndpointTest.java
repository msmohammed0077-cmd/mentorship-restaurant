package com.mentorship.restaurant.order;

import static com.mentorship.restaurant.support.RestaurantEndpointTestSupport.insertMenuItem;
import static com.mentorship.restaurant.support.RestaurantEndpointTestSupport.insertRestaurant;
import static com.mentorship.restaurant.support.RestaurantEndpointTestSupport.softDeleteRestaurant;
import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.CustomerEndpointTestSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

  @Test
  void deletesTheCartRow() {
    long customerId = insertCustomer(email("sara"));
    long cartId = insertCart(customerId);
    insertCartItem(cartId, KOFTA, 1);

    placeOrder(
            fieldsFor(
                cartId,
                addressIdForCustomer(jdbcTemplate, customerId),
                customerId,
                NILE_KITCHEN,
                "CASH_ON_DELIVERY"))
        .expectStatus()
        .isCreated();

    // #82 §2: should keep the cart row and delete only its items, currently deletes the row
    assertThat(cartExists(cartId)).isFalse();
  }

  @Test
  void leavesStockUntouched() {
    long customerId = insertCustomer(email("sara"));
    long cartId = insertCart(customerId);
    insertCartItem(cartId, KOFTA, 3);
    int stockBefore = stockFor(KOFTA);

    placeOrder(
            fieldsFor(
                cartId,
                addressIdForCustomer(jdbcTemplate, customerId),
                customerId,
                NILE_KITCHEN,
                "CASH_ON_DELIVERY"))
        .expectStatus()
        .isCreated();

    // #82 §2: should decrement stock by 3, currently unchanged
    assertThat(stockFor(KOFTA)).isEqualTo(stockBefore);
  }

  @Test
  void takesTheRestaurantFromTheCart() {
    long customerId = insertCustomer(email("sara"));
    long cartId = insertCart(customerId);
    insertCartItem(cartId, KOFTA, 1);

    placeOrder(
            fieldsFor(
                cartId,
                addressIdForCustomer(jdbcTemplate, customerId),
                customerId,
                BURGER_YARD,
                "CASH_ON_DELIVERY"))
        // #82 §2: should reject a restaurant_id that is not the cart's, currently ignores it
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.restaurant_id")
        .isEqualTo(NILE_KITCHEN);
  }

  @Test
  void treatsAMissingPaymentMethodAsCash() {
    long customerId = insertCustomer(email("sara"));
    long cartId = insertCart(customerId);
    insertCartItem(cartId, KOFTA, 1);

    placeOrder(
            fieldsFor(
                cartId,
                addressIdForCustomer(jdbcTemplate, customerId),
                customerId,
                NILE_KITCHEN,
                null))
        // #82 §2: should be 400, currently 201 with no transaction
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.transaction_response")
        .isEmpty();
  }

  @Test
  void acceptsASoftDeletedCustomer() {
    long customerId = insertCustomer(email("sara"));
    long addressId = addressIdForCustomer(jdbcTemplate, customerId);
    long cartId = insertCart(customerId);
    insertCartItem(cartId, KOFTA, 1);
    softDeleteCustomer(customerId);

    placeOrder(fieldsFor(cartId, addressId, customerId, NILE_KITCHEN, "CASH_ON_DELIVERY"))
        // #82 §2: should be 404 like the other deleted-customer guards, currently 201
        .expectStatus()
        .isCreated();
  }

  @Test
  void refusesAQuantityAboveStock() {
    long customerId = insertCustomer(email("sara"));
    long cartId = insertCart(customerId);
    insertCartItem(cartId, KOFTA, 999);

    placeOrder(
            fieldsFor(
                cartId,
                addressIdForCustomer(jdbcTemplate, customerId),
                customerId,
                NILE_KITCHEN,
                "CASH_ON_DELIVERY"))
        // #82 §2: should be 409 (OutOfStockException), currently 403
        .expectStatus()
        .isForbidden();

    assertThat(orderCountFor(customerId)).isZero();
  }

  @Test
  void refusesAnEmptyCart() {
    long customerId = insertCustomer(email("sara"));
    long cartId = insertCart(customerId);

    placeOrder(
            fieldsFor(
                cartId,
                addressIdForCustomer(jdbcTemplate, customerId),
                customerId,
                NILE_KITCHEN,
                "CASH_ON_DELIVERY"))
        // #82 §2: should be 400, currently 409
        .expectStatus()
        .isEqualTo(409);

    assertThat(orderCountFor(customerId)).isZero();
  }

  @Test
  void refusesAnUnknownCart() {
    long customerId = insertCustomer(email("sara"));

    placeOrder(
            fieldsFor(
                999999L,
                addressIdForCustomer(jdbcTemplate, customerId),
                customerId,
                NILE_KITCHEN,
                "CASH_ON_DELIVERY"))
        .expectStatus()
        .isNotFound();

    assertThat(orderCountFor(customerId)).isZero();
  }

  @Test
  void refusesAnUnknownAddress() {
    long customerId = insertCustomer(email("sara"));
    long cartId = insertCart(customerId);
    insertCartItem(cartId, KOFTA, 1);

    placeOrder(fieldsFor(cartId, 999999L, customerId, NILE_KITCHEN, "CASH_ON_DELIVERY"))
        .expectStatus()
        .isNotFound();

    assertThat(orderCountFor(customerId)).isZero();
  }

  @Test
  void refusesAnotherCustomersCart() {
    long customerId = insertCustomer(email("sara"));
    long otherCustomerId = insertCustomer(email("omar"));
    long otherCartId = insertCart(otherCustomerId);
    insertCartItem(otherCartId, KOFTA, 1);

    placeOrder(
            fieldsFor(
                otherCartId,
                addressIdForCustomer(jdbcTemplate, customerId),
                customerId,
                NILE_KITCHEN,
                "CASH_ON_DELIVERY"))
        .expectStatus()
        .isNotFound();

    assertThat(orderCountFor(customerId)).isZero();
    assertThat(cartExists(otherCartId)).isTrue();
  }

  @Test
  void refusesAnotherCustomersAddress() {
    long customerId = insertCustomer(email("sara"));
    long otherCustomerId = insertCustomer(email("omar"));
    long cartId = insertCart(customerId);
    insertCartItem(cartId, KOFTA, 1);

    placeOrder(
            fieldsFor(
                cartId,
                addressIdForCustomer(jdbcTemplate, otherCustomerId),
                customerId,
                NILE_KITCHEN,
                "CASH_ON_DELIVERY"))
        .expectStatus()
        .isNotFound();

    assertThat(orderCountFor(customerId)).isZero();
  }

  @Test
  void refusesACartFromAClosedRestaurant() {
    long restaurantId = insertRestaurant(jdbcTemplate, "Closed Grill", email("grill"), false);
    long menuItemId = insertMenuItem(jdbcTemplate, restaurantId);
    long customerId = insertCustomer(email("sara"));
    long cartId = insertCart(customerId);
    insertCartItem(cartId, menuItemId, 1);

    placeOrder(
            fieldsFor(
                cartId,
                addressIdForCustomer(jdbcTemplate, customerId),
                customerId,
                restaurantId,
                "CASH_ON_DELIVERY"))
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant is closed");

    assertThat(orderCountFor(customerId)).isZero();
    assertThat(cartExists(cartId)).isTrue();
  }

  @Test
  void refusesACartFromADeletedRestaurant() {
    long restaurantId = insertRestaurant(jdbcTemplate, "Deleted Grill", email("grill"), true);
    long menuItemId = insertMenuItem(jdbcTemplate, restaurantId);
    long customerId = insertCustomer(email("sara"));
    long cartId = insertCart(customerId);
    insertCartItem(cartId, menuItemId, 1);
    softDeleteRestaurant(jdbcTemplate, restaurantId);

    placeOrder(
            fieldsFor(
                cartId,
                addressIdForCustomer(jdbcTemplate, customerId),
                customerId,
                restaurantId,
                "CASH_ON_DELIVERY"))
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant not found");

    assertThat(orderCountFor(customerId)).isZero();
    assertThat(cartExists(cartId)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"cart_id", "address_id", "customer_id", "restaurant_id"})
  void refusesAMissingRequiredField(String field) {
    long customerId = insertCustomer(email("sara"));
    long cartId = insertCart(customerId);
    insertCartItem(cartId, KOFTA, 1);
    Map<String, Object> fields =
        fieldsFor(
            cartId,
            addressIdForCustomer(jdbcTemplate, customerId),
            customerId,
            NILE_KITCHEN,
            "CASH_ON_DELIVERY");
    fields.remove(field);

    placeOrder(fields).expectStatus().isBadRequest();

    assertThat(orderCountFor(customerId)).isZero();
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
