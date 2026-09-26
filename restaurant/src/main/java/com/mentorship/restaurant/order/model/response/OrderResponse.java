package com.mentorship.restaurant.order.model.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mentorship.restaurant.order.model.entity.OrderStatus;
import com.mentorship.restaurant.order.model.entity.RejectionReason;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.mentorship.restaurant.order.model.entity.Transaction;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

  private Long id;

  private Long customerId;

  private Long restaurantId;

  private Long addressId;

  private OrderStatus status;

  private BigDecimal total;

  private OffsetDateTime createdAt;

  private Integer prepTimeMinutes;

  private RejectionReason rejectionReason;

  private String rejectionNote;

  private String customerNote;

  private List<OrderItemResponse> orderItems;

  @JsonIgnore
  private Transaction transaction;

  private TransactionResponse transactionResponse;
}
