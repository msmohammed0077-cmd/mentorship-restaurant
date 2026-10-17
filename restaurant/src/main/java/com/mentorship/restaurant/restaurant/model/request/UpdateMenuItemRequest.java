package com.mentorship.restaurant.restaurant.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Every field is optional: null means "leave as is", so the note and image cannot be cleared here.
 * Availability has its own endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMenuItemRequest {

  @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
  @Size(max = 100)
  private String code;

  @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
  @Size(max = 150)
  private String name;

  @DecimalMin("0.00")
  @Digits(integer = 10, fraction = 2)
  private BigDecimal price;

  @PositiveOrZero private Integer stock;

  @Size(max = 255)
  private String note;

  @Size(max = 2048)
  private String imageUrl;
}
