package com.mentorship.restaurant.restaurant.model.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Every field is optional: null means "leave as is". */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMenuRequest {

  // @NotBlank would also reject null; this pattern rejects only a present-but-blank value.
  @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
  @Size(max = 100)
  private String code;

  @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
  @Size(max = 100)
  private String category;
}
