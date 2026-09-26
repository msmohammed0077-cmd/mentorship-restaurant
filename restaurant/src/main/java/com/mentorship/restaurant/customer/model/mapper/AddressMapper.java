package com.mentorship.restaurant.customer.model.mapper;

import com.mentorship.restaurant.customer.model.entity.Address;
import com.mentorship.restaurant.customer.model.response.AddressResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

  public AddressResponse toResponse(Address address) {
    return new AddressResponse(
        address.getId(),
        address.getCustomer().getId(),
        address.getLabel(),
        address.getLine(),
        address.getCity(),
        address.getArea(),
        address.getNote(),
        address.isDefault(),
        address.getCreatedAt());
  }

  public List<AddressResponse> toResponseList(List<Address> addresses) {
    return addresses.stream().map(this::toResponse).toList();
  }
}
