package com.mentorship.restaurant.restaurant.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetMenuItemAvailableRequest {

  // Boxed, like SetRestaurantOpenRequest.isOpen: a missing value must be a 400, not false.
  @NotNull private Boolean isAvailable;
}
