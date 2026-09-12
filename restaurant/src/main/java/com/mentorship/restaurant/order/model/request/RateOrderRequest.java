package com.mentorship.restaurant.order.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateOrderRequest {
  @NotNull
  @JsonProperty("customer_id")
  private Long customerId;

  @NotNull
  @Min(1)
  @Max(5)
  private Integer score;

  private String comment;
}
