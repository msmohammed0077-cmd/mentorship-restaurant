package com.mentorship.restaurant.order.model.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {
  private Long orderId;
  private String status;
  private String restaurantName;
  private Long itemCount;
  private BigDecimal total;
  private OffsetDateTime createdAt;
}
