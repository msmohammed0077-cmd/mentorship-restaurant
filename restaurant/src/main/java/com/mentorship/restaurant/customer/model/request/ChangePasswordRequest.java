package com.mentorship.restaurant.customer.model.request;

import com.mentorship.restaurant.customer.model.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

  // No stored password can exceed 72 bytes, and BCrypt refuses longer input, so a longer current
  // password is rejected up front instead of failing inside the encoder.
  @NotBlank
  @MaxUtf8Bytes(72)
  private String currentPassword;

  // BCrypt refuses passwords over 72 bytes; @Size alone counts characters, not bytes.
  @NotNull
  @Size(min = 8)
  @MaxUtf8Bytes(72)
  private String newPassword;
}
