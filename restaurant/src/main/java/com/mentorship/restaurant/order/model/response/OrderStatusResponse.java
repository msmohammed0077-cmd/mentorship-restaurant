package com.mentorship.restaurant.order.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Serialises as {@code {"order_id": 42, "status": "PREPARING"}} — the API is snake_case. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusResponse {

  private Long orderId;
  private String status;
}
