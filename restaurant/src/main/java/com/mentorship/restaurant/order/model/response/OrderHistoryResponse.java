package com.mentorship.restaurant.order.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mentorship.restaurant.order.model.OrderCursor;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderHistoryResponse {
  private List<OrderSummaryResponse> orders;

  private OrderCursor nextCursor;
}
