package com.mentorship.restaurant.order.model.request;

import com.mentorship.restaurant.order.model.entity.RejectionReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectOrderRequest {

  @NotNull private RejectionReason reason;

  /** Optional free text, for whatever the fixed set does not cover. */
  @Size(max = 255)
  private String note;
}
