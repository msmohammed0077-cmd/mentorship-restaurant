package com.mentorship.restaurant.customer.model.request;

import com.mentorship.restaurant.customer.model.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerRequest {

  @NotBlank
  @Size(max = 150)
  private String name;

  @NotBlank
  @Email
  @Size(max = 255)
  private String email;

  // BCrypt ignores everything past 72 bytes, so a longer password would be silently truncated.
  @NotNull
  @Size(min = 8, max = 72)
  private String password;

  @Pattern(regexp = "^\\+?[0-9]{7,15}$")
  private String phone;

  @Past private LocalDate dateOfBirth;

  private Gender gender;
}
