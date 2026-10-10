package com.mentorship.restaurant.restaurant.controller;

import com.mentorship.restaurant.restaurant.model.request.CreateRestaurantRequest;
import com.mentorship.restaurant.restaurant.model.request.UpdateRestaurantRequest;
import com.mentorship.restaurant.restaurant.model.response.RestaurantResponse;
import com.mentorship.restaurant.restaurant.service.RestaurantService;
import com.mentorship.restaurant.user.model.ActorRole;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/restaurants")
@Tag(name = "Restaurants")
@RequiredArgsConstructor
public class RestaurantController {

  private final RestaurantService restaurantService;

  @PostMapping
  public ResponseEntity<RestaurantResponse> createRestaurant(
      @RequestParam ActorRole role, @Valid @RequestBody CreateRestaurantRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(restaurantService.createRestaurant(role, request));
  }

  @GetMapping
  public ResponseEntity<PagedModel<RestaurantResponse>> listRestaurants(
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(restaurantService.listRestaurants(pageable));
  }

  @GetMapping("/{restaurantId}")
  public ResponseEntity<RestaurantResponse> getRestaurant(@PathVariable Long restaurantId) {
    return ResponseEntity.ok(restaurantService.getRestaurant(restaurantId));
  }

  /** {@code restaurantId} in the query is the caller's claimed identity, for role RESTAURANT. */
  @PatchMapping("/{restaurantId}")
  public ResponseEntity<RestaurantResponse> updateRestaurant(
      @PathVariable Long restaurantId,
      @RequestParam ActorRole role,
      @RequestParam(name = "restaurantId", required = false) Long callerRestaurantId,
      @Valid @RequestBody UpdateRestaurantRequest request) {
    return ResponseEntity.ok(
        restaurantService.updateRestaurant(restaurantId, role, callerRestaurantId, request));
  }
}
