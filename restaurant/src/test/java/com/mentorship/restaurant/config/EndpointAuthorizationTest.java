package com.mentorship.restaurant.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import com.mentorship.restaurant.support.JwtTestSupport;
import com.mentorship.restaurant.support.RealmPermissions;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The permission matrix (#83 part 4, ADR 0008). Every protected endpoint refuses a request without
 * a token (401) or without its permission (403), and lets one holding it through. Requests are
 * valid, because validation runs before the permission check and would answer 400 first; ids do not
 * exist, so a permitted request ends in a 404 and changes nothing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class EndpointAuthorizationTest {

  private static final long NONE = 999_999L;
  private static final UUID USER = JwtTestSupport.ADMIN;
  private static final String AS_RESTAURANT = "?restaurantId=" + NONE;

  /** Endpoints reachable without a token (SecurityConfig); Swagger and /error are not ours. */
  private static final Set<String> PUBLIC =
      Set.of(
          "GET /api/v1/health",
          "GET /api/v1/restaurants",
          "GET /api/v1/restaurants/{restaurantId}",
          "GET /api/v1/restaurants/{restaurantId}/menu");

  private static final String ADDRESS =
      """
      {"label": "Home", "line": "12 Tahrir Street", "city": "Cairo", "area": "Dokki"}
      """;
  private static final String CARD =
      """
      {"brand": "VISA", "last4": "4242", "expiry_month": 12, "expiry_year": 2099,
       "holder_name": "Sara Youssef"}
      """;
  private static final String CART_ITEM =
      """
      {"menu_item_id": 1, "quantity": 1}
      """;
  private static final String QUANTITY =
      """
      {"quantity": 1}
      """;
  private static final String ORDER =
      """
      {"cart_id": %d, "address_id": %d, "restaurant_id": 1}
      """
          .formatted(NONE, NONE);
  private static final String RATING =
      """
      {"score": 5}
      """;

  /** Names the admin's own Keycloak user, already linked, so a permitted create is a 409. */
  private static final String RESTAURANT =
      """
      {"name": "Koshary Corner", "email": "admin@example.com", "owner_keycloak_id": "%s"}
      """
          .formatted(USER);

  private static final String OPEN =
      """
      {"is_open": true}
      """;
  private static final String MENU =
      """
      {"code": "AUTHZ-MENU", "category": "Main"}
      """;
  private static final String MENU_ITEM =
      """
      {"code": "AUTHZ-ITEM", "name": "Kofta", "price": 10.00, "stock": 1}
      """;
  private static final String AVAILABLE =
      """
      {"is_available": true}
      """;
  private static final String REJECTION =
      """
      {"reason": "TOO_BUSY"}
      """;

  @Autowired private RestTestClient client;
  @Autowired private JwtTestSupport jwts;

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping handlerMapping;

  /** One protected endpoint: its mapping, a valid request to it, and the permission it needs. */
  record Row(HttpMethod method, String pattern, String uri, String body, String permission) {
    String endpoint() {
      return method + " " + pattern;
    }

    @Override
    public String toString() {
      return endpoint();
    }
  }

  static List<Row> rows() {
    return List.of(
        row(GET, "/api/v1/customers/me", "", null, "customer.read"),
        row(PATCH, "/api/v1/customers/me", "", "{}", "customer.write"),
        row(DELETE, "/api/v1/customers/me", "", null, "customer.write"),
        row(POST, "/api/v1/addresses", "", ADDRESS, "address.write"),
        row(GET, "/api/v1/addresses", "", null, "address.read"),
        row(PUT, "/api/v1/addresses/{addressId}", "", ADDRESS, "address.write"),
        row(DELETE, "/api/v1/addresses/{addressId}", "", null, "address.write"),
        row(PUT, "/api/v1/addresses/{addressId}/default", "", null, "address.write"),
        row(POST, "/api/v1/payment-methods", "", CARD, "payment-method.write"),
        row(GET, "/api/v1/payment-methods", "", null, "payment-method.read"),
        row(
            PUT,
            "/api/v1/payment-methods/{paymentMethodId}/default",
            "",
            null,
            "payment-method.write"),
        row(DELETE, "/api/v1/payment-methods/{paymentMethodId}", "", null, "payment-method.write"),
        row(POST, "/api/v1/cart/items", "", CART_ITEM, "cart.write"),
        row(PUT, "/api/v1/cart/{cartId}/items/{cartItemId}", "", QUANTITY, "cart.write"),
        row(GET, "/api/v1/cart/{cartId}", "", null, "cart.read"),
        row(DELETE, "/api/v1/cart/{cartId}", "", null, "cart.write"),
        row(DELETE, "/api/v1/cart/{cartId}/items", "?cartItemIds=1", null, "cart.write"),
        row(POST, "/api/v1/cart/{cartId}/checkout", "", null, "cart.write"),
        row(POST, "/api/v1/orders", "", ORDER, "order.create"),
        row(GET, "/api/v1/orders/{orderId}", "", null, "order.read"),
        row(GET, "/api/v1/orders", "", null, "order.read"),
        row(POST, "/api/v1/orders/{orderId}/cancel", "", null, "order.cancel"),
        row(POST, "/api/v1/orders/{orderId}/rating", "", RATING, "order.rate"),
        row(POST, "/api/v1/orders/{orderId}/accept", AS_RESTAURANT, null, "order.accept"),
        row(POST, "/api/v1/orders/{orderId}/reject", AS_RESTAURANT, REJECTION, "order.reject"),
        row(POST, "/api/v1/orders/{orderId}/preparing", AS_RESTAURANT, null, "order.prepare"),
        row(
            POST,
            "/api/v1/orders/{orderId}/ready-for-pickup",
            AS_RESTAURANT,
            null,
            "order.prepare"),
        row(POST, "/api/v1/restaurants", "", RESTAURANT, "restaurant.create"),
        row(PATCH, "/api/v1/restaurants/{restaurantId}", "", "{}", "restaurant.write"),
        row(PUT, "/api/v1/restaurants/{restaurantId}/open", "", OPEN, "restaurant.write"),
        row(DELETE, "/api/v1/restaurants/{restaurantId}", "", null, "restaurant.delete"),
        row(POST, "/api/v1/restaurants/{restaurantId}/menu", "", MENU, "menu.write"),
        row(PATCH, "/api/v1/restaurants/{restaurantId}/menu", "", "{}", "menu.write"),
        row(DELETE, "/api/v1/restaurants/{restaurantId}/menu", "", null, "menu.write"),
        row(POST, "/api/v1/restaurants/{restaurantId}/menu/items", "", MENU_ITEM, "menu.write"),
        row(
            PATCH,
            "/api/v1/restaurants/{restaurantId}/menu/items/{menuItemId}",
            "",
            "{}",
            "menu.write"),
        row(
            PUT,
            "/api/v1/restaurants/{restaurantId}/menu/items/{menuItemId}/available",
            "",
            AVAILABLE,
            "menu.write"),
        row(
            DELETE,
            "/api/v1/restaurants/{restaurantId}/menu/items/{menuItemId}",
            "",
            null,
            "menu.write"));
  }

  /** Every path variable becomes an id that does not exist. */
  private static Row row(
      HttpMethod method, String pattern, String query, String body, String permission) {
    String uri = pattern.replaceAll("\\{[^}]+}", String.valueOf(NONE)) + query;
    return new Row(method, pattern, uri, body, permission);
  }

  @ParameterizedTest
  @MethodSource("rows")
  void refusesARequestWithoutAToken(Row row) {
    send(row, null)
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .valueMatches(HttpHeaders.WWW_AUTHENTICATE, "Bearer.*");
  }

  @ParameterizedTest
  @MethodSource("rows")
  void refusesATokenWithoutThePermission(Row row) {
    Set<String> everyOther = new TreeSet<>(RealmPermissions.all());
    everyOther.remove(row.permission());

    send(row, jwts.tokenWithPermissions(USER, everyOther))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Access denied");
  }

  @ParameterizedTest
  @MethodSource("rows")
  void letsTheirPermissionThrough(Row row) {
    EntityExchangeResult<String> result =
        send(row, jwts.tokenWithPermissions(USER, Set.of(row.permission())))
            .expectBody(String.class)
            .returnResult();

    // Ownership guards answer 403 too (the #82 pins); only ours says "Access denied".
    int status = result.getStatus().value();
    String body = String.valueOf(result.getResponseBody());
    assertThat(status).as(row.endpoint()).isNotEqualTo(401);
    assertThat(status).as(row.endpoint()).isLessThan(500);
    assertThat(status == 403 && body.contains("Access denied")).as(row.endpoint()).isFalse();
  }

  /** Sign-up moved to Keycloak (#83 part 3): the old registration path is not public any more. */
  @Test
  void theOldRegistrationEndpointNeedsAToken() {
    client
        .post()
        .uri("/api/v1/customers")
        .contentType(MediaType.APPLICATION_JSON)
        .body("{}")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void aTokenWithoutPermissionsIsForbidden() {
    client
        .get()
        .uri("/api/v1/customers/me")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwts.tokenFor(USER))
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectHeader()
        .valueMatches(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"insufficient_scope\".*")
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Access denied");
  }

  /**
   * An endpoint without a row would be open to any logged-in caller if it also lacked
   * {@code @PreAuthorize}, since the fallback is only {@code authenticated()}.
   */
  @Test
  void everyEndpointOfOursIsPublicOrInTheMatrix() {
    Set<String> ours = new TreeSet<>();
    handlerMapping
        .getHandlerMethods()
        .forEach(
            (info, handler) -> {
              if (handler.getBeanType().getPackageName().startsWith("com.mentorship.restaurant")) {
                // No method condition means every method: "ANY" matches no row, so it fails.
                Set<String> methods =
                    info.getMethodsCondition().getMethods().isEmpty()
                        ? Set.of("ANY")
                        : info.getMethodsCondition().getMethods().stream()
                            .map(Object::toString)
                            .collect(Collectors.toSet());
                methods.forEach(
                    method ->
                        info.getPathPatternsCondition()
                            .getPatternValues()
                            .forEach(pattern -> ours.add(method + " " + pattern)));
              }
            });
    Set<String> covered =
        rows().stream().map(Row::endpoint).collect(Collectors.toCollection(TreeSet::new));
    covered.addAll(PUBLIC);

    assertThat(covered).containsExactlyInAnyOrderElementsOf(ours);
  }

  @Test
  void theMatrixAndTheRealmNameTheSamePermissions() {
    Set<String> used = rows().stream().map(Row::permission).collect(Collectors.toSet());

    assertThat(used).containsExactlyInAnyOrderElementsOf(RealmPermissions.all());
  }

  private RestTestClient.ResponseSpec send(Row row, String token) {
    RestTestClient.RequestBodySpec request = client.method(row.method()).uri(row.uri());
    if (token != null) {
      request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
    if (row.body() != null) {
      request.contentType(MediaType.APPLICATION_JSON).body(row.body());
    }
    return request.exchange();
  }
}
