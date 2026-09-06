package com.mentorship.restaurant.customer.service;

import com.mentorship.restaurant.customer.model.request.AddAddressRequest;
import com.mentorship.restaurant.customer.model.response.AddressResponse;
import com.mentorship.restaurant.customer.service.handler.AddAddressHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AddressService {

  private final AddAddressHandler addAddressHandler;

  public AddressResponse addAddress(Long customerId, AddAddressRequest request) {
    return addAddressHandler.addAddress(customerId, request);
  }
}
