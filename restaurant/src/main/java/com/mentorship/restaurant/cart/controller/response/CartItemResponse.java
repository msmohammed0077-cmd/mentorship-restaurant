package com.mentorship.restaurant.cart.controller.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

  private Long id;
  private Long menuItemId;
  private String itemName;
  private String note;
  private BigDecimal itemPrice;
  private Integer quantity;
}
