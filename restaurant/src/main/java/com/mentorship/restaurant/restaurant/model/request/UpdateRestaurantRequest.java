package com.mentorship.restaurant.restaurant.model.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Every field is optional: null means "leave as is", so a field cannot be cleared here. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRestaurantRequest {

  // @NotBlank would also reject null; this pattern rejects only a present-but-blank value.
  // Stored as both the restaurant's name and its user's name; user_name is VARCHAR(150).
  @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
  @Size(max = 150)
  private String name;

  // restaurant_description is TEXT; the cap keeps every list page's size bounded.
  @Size(max = 1000)
  private String description;
}
