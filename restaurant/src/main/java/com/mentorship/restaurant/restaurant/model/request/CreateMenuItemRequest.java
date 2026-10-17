package com.mentorship.restaurant.restaurant.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMenuItemRequest {

  @NotBlank
  @Size(max = 100)
  private String code;

  @NotBlank
  @Size(max = 150)
  private String name;

  // menu_item_price is NUMERIC(12, 2) and CHECKs >= 0.
  @NotNull
  @DecimalMin("0.00")
  @Digits(integer = 10, fraction = 2)
  private BigDecimal price;

  @NotNull @PositiveOrZero private Integer stock;

  @Size(max = 255)
  private String note;

  // menu_item_image_url is TEXT; the cap keeps a menu's size bounded. Images themselves are #101.
  @Size(max = 2048)
  private String imageUrl;

  /** Null means available. */
  private Boolean isAvailable;
}
