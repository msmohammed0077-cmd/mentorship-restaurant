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

  @NotNull
  @Positive
  @Max(1000)
  private Integer quantity;

  private String note;
}
