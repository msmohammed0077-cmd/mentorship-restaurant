package com.mentorship.restaurant.customer.model.response;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

  private Long id;
  private Long customerId;
  private String label;
  private String line;
  private String city;
  private String area;
  private String note;
  private Boolean isDefault;
  private OffsetDateTime createdAt;
}
