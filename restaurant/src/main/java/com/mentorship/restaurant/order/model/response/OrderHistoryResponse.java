package com.mentorship.restaurant.order.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Omits next_cursor entirely on the last page rather than serialising it as null. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderHistoryResponse {

  private List<OrderSummaryResponse> orders;

  /** Null on the last page. Keyset paging has no total count and no page numbers. */
  private String nextCursor;
}
