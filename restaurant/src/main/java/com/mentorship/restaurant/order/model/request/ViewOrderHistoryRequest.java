package com.mentorship.restaurant.order.model.request;

import com.mentorship.restaurant.order.model.entity.ActorRole;
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

  @NotNull private ActorRole role;

  /**
   * The initialiser is the default when the parameter is absent. @NotNull covers the other case: an
   * empty {@code ?limit=} binds null <i>over</i> the default, and null passes @Min/@Max.
   */
  @NotNull
  @Min(1)
  @Max(50)
  private Integer limit = 20;

  private String cursor;
}
