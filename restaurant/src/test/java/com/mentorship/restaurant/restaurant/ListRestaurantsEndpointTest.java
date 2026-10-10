package com.mentorship.restaurant.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListRestaurantsEndpointTest extends RestaurantEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "list.restaurants.test.";
  }

  /** Seeds are global and other classes may add rows, so counts are relative to this. */
  private int activeRestaurants() {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM restaurants r JOIN users u ON u.user_id = r.user_id
            WHERE u.user_deleted_at IS NULL
            """,
            Integer.class);
    return count == null ? 0 : count;
  }

  private static List<Long> asLongs(List<?> ids) {
    return ids.stream().map(id -> ((Number) id).longValue()).toList();
  }

  @Test
  void returnsTheFirstPageWithDefaultSize() {
    client
        .get()
        .uri("/api/v1/restaurants")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.content[0].restaurant_id")
        .isEqualTo(1)
        .jsonPath("$.content[0].name")
        .isEqualTo("Nile Kitchen")
        .jsonPath("$.content[0].password")
        .doesNotExist()
        .jsonPath("$.page.size")
        .isEqualTo(20)
        .jsonPath("$.page.number")
        .isEqualTo(0)
        .jsonPath("$.page.total_elements")
        .isEqualTo(activeRestaurants());
  }

  @Test
  void listsOpenAndClosedRestaurantsAndPagesThroughThem() {
    int baseline = activeRestaurants();
    long open = insertRestaurant("Koshary Corner", email("koshary"), true);
    long closed = insertRestaurant("Fatta Hall", email("fatta"), false);

    // Ids ascend, so the two new rows are the last two of the list.
    client
        .get()
        .uri("/api/v1/restaurants?size=1&page={page}", baseline)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.content.length()")
        .isEqualTo(1)
        .jsonPath("$.content[0].restaurant_id")
        .isEqualTo(open)
        .jsonPath("$.content[0].is_open")
        .isEqualTo(true)
        .jsonPath("$.page.number")
        .isEqualTo(baseline)
        .jsonPath("$.page.total_elements")
        .isEqualTo(baseline + 2)
        .jsonPath("$.page.total_pages")
        .isEqualTo(baseline + 2);

    client
        .get()
        .uri("/api/v1/restaurants?size=1&page={page}", baseline + 1)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.content[0].restaurant_id")
        .isEqualTo(closed)
        .jsonPath("$.content[0].is_open")
        .isEqualTo(false);
  }

  @Test
  void clampsTheSizeToFifty() {
    client
        .get()
        .uri("/api/v1/restaurants?size=500")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.page.size")
        .isEqualTo(50);
  }

  @Test
  void treatsANegativePageAsTheFirst() {
    client
        .get()
        .uri("/api/v1/restaurants?page=-1")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.page.number")
        .isEqualTo(0);
  }

  @Test
  void ignoresTheClientsSort() {
    insertRestaurant("Aaa First By Name", email("aaa"), true);

    client
        .get()
        .uri("/api/v1/restaurants?size=50&sort=name,asc")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.content[*].restaurant_id")
        .value(List.class, ids -> assertThat(asLongs(ids)).isSorted());
  }

  @Test
  void excludesSoftDeletedRestaurants() {
    int baseline = activeRestaurants();
    long kept = insertRestaurant("Koshary Corner", email("koshary"), true);
    long deleted = insertRestaurant("Fatta Hall", email("fatta"), true);
    softDeleteRestaurant(deleted);

    client
        .get()
        .uri("/api/v1/restaurants?size=50")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.page.total_elements")
        .isEqualTo(baseline + 1)
        .jsonPath("$.content[*].restaurant_id")
        .value(List.class, ids -> assertThat(asLongs(ids)).contains(kept).doesNotContain(deleted));
  }
}
