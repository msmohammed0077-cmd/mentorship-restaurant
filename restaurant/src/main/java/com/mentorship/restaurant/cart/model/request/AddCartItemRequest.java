package com.mentorship.restaurant.cart.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddCartItemRequest {

  @NotNull private Long customerId;

  @NotNull private Long menuItemId;

  // Max keeps the handler's int arithmetic safe: without it a quantity of
  // Integer.MAX_VALUE would wrap negative when added to the existing line's
  // quantity and slip past the stock check.
  @NotNull
  @Positive
  @Max(1000)
  private Integer quantity;

  private String note;
}
