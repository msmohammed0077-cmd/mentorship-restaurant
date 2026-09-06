package com.mentorship.restaurant.exception;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

  private OffsetDateTime timestamp;
  private int status;
  private String error;
  private String message;
  private String path;
}
