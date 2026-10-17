package com.mentorship.restaurant.restaurant.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetRestaurantOpenRequest {

  // Boxed, like RestaurantResponse.isOpen: a primitive's isOpen()/setOpen() would bind "open",
  // and a primitive cannot be null, so a missing value would silently mean false.
  @NotNull private Boolean isOpen;
}
