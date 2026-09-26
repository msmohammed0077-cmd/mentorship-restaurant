package com.mentorship.restaurant.cart.model.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

  private Long id;
  private Long customerId;
  private List<CartItemResponse> items;
  private BigDecimal total;
}
