package com.mentorship.restaurant.restaurant.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMenuRequest {

  // menu_code and menu_category are VARCHAR(100).
  @NotBlank
  @Size(max = 100)
  private String code;

  @NotBlank
  @Size(max = 100)
  private String category;
}
