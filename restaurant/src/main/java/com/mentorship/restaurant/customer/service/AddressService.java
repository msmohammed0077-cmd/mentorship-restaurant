package com.mentorship.restaurant.customer.service;

import com.mentorship.restaurant.customer.model.request.AddAddressRequest;
import com.mentorship.restaurant.customer.model.response.AddressResponse;
import com.mentorship.restaurant.customer.service.handler.AddAddressHandler;
import com.mentorship.restaurant.customer.service.handler.SetDefaultAddressHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AddressService {

  private final AddAddressHandler addAddressHandler;
  private final SetDefaultAddressHandler setDefaultAddressHandler;

  public AddressResponse addAddress(Long customerId, AddAddressRequest request) {
    return addAddressHandler.addAddress(customerId, request);
  }

  public AddressResponse setDefaultAddress(Long customerId, Long addressId) {
    return setDefaultAddressHandler.setDefaultAddress(customerId, addressId);
  }
}
