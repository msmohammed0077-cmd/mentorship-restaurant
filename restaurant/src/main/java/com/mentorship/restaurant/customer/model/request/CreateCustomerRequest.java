package com.mentorship.restaurant.customer.model.request;

import com.mentorship.restaurant.customer.model.entity.Gender;
import com.mentorship.restaurant.customer.model.validation.MaxUtf8Bytes;
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

  // BCrypt refuses passwords over 72 bytes, and a multi-byte character counts as several, so
  // the character count alone cannot guarantee the encoder will accept it.
  @NotNull
  @Size(min = 8)
  @MaxUtf8Bytes(72)
  private String password;

  @Pattern(regexp = "^\\+?[0-9]{7,15}$")
  private String phone;

  @Past private LocalDate dateOfBirth;

  private Gender gender;
}
