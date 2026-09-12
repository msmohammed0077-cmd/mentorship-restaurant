package com.mentorship.restaurant.order.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

  @NotNull private Long cartId;

  @NotNull private Long addressId;

  private String customerNote;

  private String promoCode;

  private String cardId;

  private PaymentMethod paymentMethod;
}
