package com.mentorship.restaurant.order.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRatingResponse {
  @JsonProperty("rating_id")
  private Long ratingId;

  @JsonProperty("order_id")
  private Long orderId;

  private Integer score;

  private String comment;

  @JsonProperty("created_at")
  private OffsetDateTime createdAt;
}
