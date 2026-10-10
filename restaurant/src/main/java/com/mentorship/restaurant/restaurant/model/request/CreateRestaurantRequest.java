package com.mentorship.restaurant.restaurant.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRestaurantRequest {

  // Stored as both the restaurant's name and its user's name; user_name is VARCHAR(150).
  @NotBlank
  @Size(max = 150)
  private String name;

  private String description;

  @NotBlank
  @Email
  @Size(max = 255)
  private String email;
}
