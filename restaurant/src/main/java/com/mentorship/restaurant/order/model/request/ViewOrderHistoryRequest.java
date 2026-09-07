package com.mentorship.restaurant.order.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViewOrderHistoryRequest {

  @NotNull private Long customerId;

  @Min(1)
  @Max(50)
  private Integer limit = 20;

  private String cursor;
}
