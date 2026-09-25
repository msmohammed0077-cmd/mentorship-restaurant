package com.mentorship.restaurant.customer.model.response;

import com.mentorship.restaurant.customer.model.entity.Gender;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

  private Long customerId;
  private String name;
  private String email;
  private String phone;
  private LocalDate dateOfBirth;
  private Gender gender;
}
