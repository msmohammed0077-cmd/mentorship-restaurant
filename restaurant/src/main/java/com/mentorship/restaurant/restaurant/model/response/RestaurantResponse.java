package com.mentorship.restaurant.restaurant.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantResponse {

  private Long restaurantId;
  private String name;
  private String description;
  private String email;

  // Boxed, like AddressResponse.isDefault: Lombok names a primitive boolean's getter isOpen(),
  // which Jackson would serialise as "open".
  private Boolean isOpen;
}
