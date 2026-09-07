package com.mentorship.restaurant.order.model.request;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Binds {@code {"prep_time_minutes": 25}} — the API is snake_case. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcceptOrderRequest {

  /** Optional, and nothing consumes it. It is what makes a delivery ETA possible later. */
  @Positive private Integer prepTimeMinutes;
}
