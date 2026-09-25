package com.mentorship.restaurant.customer.model.request;

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

  @NotBlank private String currentPassword;

  // BCrypt ignores everything past 72 bytes, so a longer password would be silently truncated.
  @NotNull
  @Size(min = 8, max = 72)
  private String newPassword;
}
