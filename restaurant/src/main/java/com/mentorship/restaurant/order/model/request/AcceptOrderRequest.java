package com.mentorship.restaurant.order.model.request;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcceptOrderRequest {
  @Positive private Integer prepTimeMinutes;
}
