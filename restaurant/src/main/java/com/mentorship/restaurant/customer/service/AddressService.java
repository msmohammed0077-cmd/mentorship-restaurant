package com.mentorship.restaurant.customer.service;

import com.mentorship.restaurant.customer.model.request.AddAddressRequest;
import com.mentorship.restaurant.customer.model.request.UpdateAddressRequest;
import com.mentorship.restaurant.customer.model.response.AddressResponse;
import com.mentorship.restaurant.customer.service.handler.AddAddressHandler;
import com.mentorship.restaurant.customer.service.handler.DeleteAddressHandler;
import com.mentorship.restaurant.customer.service.handler.SetDefaultAddressHandler;
import com.mentorship.restaurant.customer.service.handler.UpdateAddressHandler;
import com.mentorship.restaurant.customer.service.handler.ViewAddressesHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AddressService {

  private final AddAddressHandler addAddressHandler;
  private final SetDefaultAddressHandler setDefaultAddressHandler;
  private final ViewAddressesHandler viewAddressesHandler;
  private final UpdateAddressHandler updateAddressHandler;
  private final DeleteAddressHandler deleteAddressHandler;

  public AddressResponse addAddress(Long customerId, AddAddressRequest request) {
    return addAddressHandler.addAddress(customerId, request);
  }

  public List<AddressResponse> viewAddresses(Long customerId) {
    return viewAddressesHandler.viewAddresses(customerId);
  }

  public AddressResponse updateAddress(
      Long customerId, Long addressId, UpdateAddressRequest request) {
    return updateAddressHandler.updateAddress(customerId, addressId, request);
  }

  public void deleteAddress(Long customerId, Long addressId) {
    deleteAddressHandler.deleteAddress(customerId, addressId);
  }

  public AddressResponse setDefaultAddress(Long customerId, Long addressId) {
    return setDefaultAddressHandler.setDefaultAddress(customerId, addressId);
  }
}
