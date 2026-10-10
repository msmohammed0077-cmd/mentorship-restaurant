package com.mentorship.restaurant.restaurant.model.mapper;

import com.mentorship.restaurant.restaurant.model.entity.Restaurant;
import com.mentorship.restaurant.restaurant.model.response.RestaurantResponse;
import org.springframework.stereotype.Component;

@Component
public class RestaurantMapper {

  public RestaurantResponse toResponse(Restaurant restaurant) {
    return new RestaurantResponse(
        restaurant.getId(),
        restaurant.getRestaurantName(),
        restaurant.getRestaurantDescription(),
        restaurant.getUser().getUserEmail(),
        restaurant.isOpen());
  }
}
