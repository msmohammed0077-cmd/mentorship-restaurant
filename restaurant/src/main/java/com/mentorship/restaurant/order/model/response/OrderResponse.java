package com.mentorship.restaurant.order.model.response;

import com.mentorship.restaurant.order.model.entity.OrderStatus;
import com.mentorship.restaurant.order.model.entity.RejectionReason;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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

  private TransactionResponse transactions;
}
