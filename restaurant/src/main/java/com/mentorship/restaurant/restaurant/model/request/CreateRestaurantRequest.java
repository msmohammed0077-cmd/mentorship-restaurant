package com.mentorship.restaurant.restaurant.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
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

  // restaurant_description is TEXT; the cap keeps every list page's size bounded.
  @Size(max = 1000)
  private String description;

  @NotBlank
  @Email
  @Size(max = 255)
  private String email;

  // The owner's Keycloak user, created by the admin first; it is what their token's sub will be.
  @NotNull private UUID ownerKeycloakId;
}
