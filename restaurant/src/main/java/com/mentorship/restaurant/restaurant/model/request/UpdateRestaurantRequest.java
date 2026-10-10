package com.mentorship.restaurant.restaurant.model.request;

import jakarta.validation.constraints.Email;
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

  private String description;

  // @Email accepts an empty string, so the same pattern keeps "" from being stored as the email.
  @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
  @Email
  @Size(max = 255)
  private String email;
}
