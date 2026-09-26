package com.mentorship.restaurant.customer.model.mapper;

import com.mentorship.restaurant.customer.model.entity.Customer;
import com.mentorship.restaurant.customer.model.entity.User;
import com.mentorship.restaurant.customer.model.response.CustomerResponse;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

  public CustomerResponse toResponse(Customer customer) {
    User user = customer.getUser();
    return new CustomerResponse(
        customer.getId(),
        user.getUserName(),
        user.getUserEmail(),
        user.getUserPhone(),
        user.getUserDateOfBirth(),
        user.getUserGender());
  }
}
