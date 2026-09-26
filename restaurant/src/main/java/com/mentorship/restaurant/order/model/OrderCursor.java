package com.mentorship.restaurant.order.model;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCursor {
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  private Long orderId;

  public boolean isAbsent() {
    return createdAt == null && orderId == null;
  }

  public boolean isPartial() {
    return (createdAt == null) != (orderId == null);
  }
}
