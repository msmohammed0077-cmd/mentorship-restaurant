package com.mentorship.restaurant.customer.model.request;

import com.mentorship.restaurant.customer.model.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Every field is optional: null means "leave as is", so a field cannot be cleared here. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerRequest {

  // @NotBlank would also reject null; this pattern rejects only a present-but-blank value.
  @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
  @Size(max = 150)
  private String name;

  // @Email accepts an empty string, so the same pattern keeps "" from being stored as the email.
  @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
  @Email
  @Size(max = 255)
  private String email;

  @Pattern(regexp = "^\\+?[0-9]{7,15}$")
  private String phone;

  @Past private LocalDate dateOfBirth;

  private Gender gender;
}
