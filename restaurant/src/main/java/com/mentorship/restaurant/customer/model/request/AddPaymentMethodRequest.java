package com.mentorship.restaurant.customer.model.request;

import com.mentorship.restaurant.customer.model.entity.CardBrand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Only what a "saved cards" screen shows: never the full card number or the CVV. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddPaymentMethodRequest {

  @NotNull private CardBrand brand;

  @NotNull
  @Pattern(regexp = "^[0-9]{4}$")
  private String last4;

  @NotNull
  @Min(1)
  @Max(12)
  private Short expiryMonth;

  @NotNull
  @Min(2000)
  @Max(2100)
  private Short expiryYear;

  @NotBlank
  @Size(max = 150)
  private String holderName;
}
