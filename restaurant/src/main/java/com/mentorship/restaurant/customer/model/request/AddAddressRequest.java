package com.mentorship.restaurant.customer.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddAddressRequest {

  @NotBlank
  @Size(max = 100)
  private String label;

  @NotBlank
  @Size(max = 255)
  private String line;

  @NotBlank
  @Size(max = 100)
  private String city;

  @NotBlank
  @Size(max = 100)
  private String area;

  private String note;
}
