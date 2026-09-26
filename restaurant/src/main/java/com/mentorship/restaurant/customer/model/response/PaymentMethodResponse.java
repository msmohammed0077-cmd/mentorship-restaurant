package com.mentorship.restaurant.customer.model.response;

import com.mentorship.restaurant.customer.model.entity.CardBrand;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponse {

  private Long paymentMethodId;
  private CardBrand brand;
  private String last4;
  private Short expiryMonth;
  private Short expiryYear;
  private String holderName;
  private Boolean isDefault;
}
