package com.mentorship.restaurant.restaurant.controller;

import com.mentorship.restaurant.restaurant.model.response.RestaurantResponse;
import com.mentorship.restaurant.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/restaurants")
@Tag(name = "Restaurants")
@RequiredArgsConstructor
public class RestaurantController {

  private final RestaurantService restaurantService;

  @GetMapping("/{restaurantId}")
  public ResponseEntity<RestaurantResponse> getRestaurant(@PathVariable Long restaurantId) {
    return ResponseEntity.ok(restaurantService.getRestaurant(restaurantId));
  }
}
