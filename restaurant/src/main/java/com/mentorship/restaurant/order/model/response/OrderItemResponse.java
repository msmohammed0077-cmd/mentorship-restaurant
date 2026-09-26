package com.mentorship.restaurant.order.model.response;

import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

  private Long id;

  private Long menuItemId;

  private String itemName;

  private Integer quantity;

  private BigDecimal itemPrice;

  private BigDecimal totalPrice;
}
