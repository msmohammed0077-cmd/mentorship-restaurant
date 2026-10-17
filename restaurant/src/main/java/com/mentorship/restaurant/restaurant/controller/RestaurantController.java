package com.mentorship.restaurant.restaurant.controller;

import com.mentorship.restaurant.config.PublicEndpoint;
import com.mentorship.restaurant.restaurant.model.request.CreateRestaurantRequest;
import com.mentorship.restaurant.restaurant.model.request.SetRestaurantOpenRequest;
import com.mentorship.restaurant.restaurant.model.request.UpdateRestaurantRequest;
import com.mentorship.restaurant.restaurant.model.response.RestaurantResponse;
import com.mentorship.restaurant.restaurant.service.RestaurantDeletionService;
import com.mentorship.restaurant.restaurant.service.RestaurantService;
import com.mentorship.restaurant.user.model.Caller;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/restaurants")
@Tag(name = "Restaurants")
@RequiredArgsConstructor
public class RestaurantController {

  private final RestaurantService restaurantService;
  private final RestaurantDeletionService restaurantDeletionService;

  @PreAuthorize("hasAuthority('restaurant.create')")
  @PostMapping
  public ResponseEntity<RestaurantResponse> createRestaurant(
      @Valid @RequestBody CreateRestaurantRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(restaurantService.createRestaurant(request));
  }

  @PublicEndpoint
  @GetMapping
  public ResponseEntity<PagedModel<RestaurantResponse>> listRestaurants(
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(restaurantService.listRestaurants(pageable));
  }

  @PublicEndpoint
  @GetMapping("/{restaurantId}")
  public ResponseEntity<RestaurantResponse> getRestaurant(@PathVariable Long restaurantId) {
    return ResponseEntity.ok(restaurantService.getRestaurant(restaurantId));
  }

  @PreAuthorize("hasAuthority('restaurant.write')")
  @PatchMapping("/{restaurantId}")
  public ResponseEntity<RestaurantResponse> updateRestaurant(
      @AuthenticationPrincipal Caller caller,
      @PathVariable Long restaurantId,
      @Valid @RequestBody UpdateRestaurantRequest request) {
    return ResponseEntity.ok(
        restaurantService.updateRestaurant(restaurantId, caller.userId(), request));
  }

  @PreAuthorize("hasAuthority('restaurant.write')")
  @PutMapping("/{restaurantId}/open")
  public ResponseEntity<RestaurantResponse> setRestaurantOpen(
      @AuthenticationPrincipal Caller caller,
      @PathVariable Long restaurantId,
      @Valid @RequestBody SetRestaurantOpenRequest request) {
    return ResponseEntity.ok(
        restaurantService.setRestaurantOpen(restaurantId, caller.userId(), request));
  }

  @PreAuthorize("hasAuthority('restaurant.delete')")
  @DeleteMapping("/{restaurantId}")
  public ResponseEntity<Void> deleteRestaurant(@PathVariable Long restaurantId) {
    restaurantDeletionService.deleteRestaurant(restaurantId);
    return ResponseEntity.noContent().build();
  }
}
