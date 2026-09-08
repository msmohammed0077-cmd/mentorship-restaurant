package com.mentorship.restaurant.customer.controller;

import com.mentorship.restaurant.customer.model.request.AddAddressRequest;
import com.mentorship.restaurant.customer.model.request.UpdateAddressRequest;
import com.mentorship.restaurant.customer.model.response.AddressResponse;
import com.mentorship.restaurant.customer.service.AddressService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/addresses")
@Tag(name = "Addresses")
@RequiredArgsConstructor
public class AddressController {

  private final AddressService addressService;

  @PostMapping
  public ResponseEntity<AddressResponse> addAddress(
      @RequestParam Long customerId, @Valid @RequestBody AddAddressRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(addressService.addAddress(customerId, request));
  }

  @GetMapping
  public ResponseEntity<List<AddressResponse>> viewAddresses(@RequestParam Long customerId) {
    return ResponseEntity.ok(addressService.viewAddresses(customerId));
  }

  @PutMapping("/{addressId}")
  public ResponseEntity<AddressResponse> updateAddress(
      @RequestParam Long customerId,
      @PathVariable Long addressId,
      @Valid @RequestBody UpdateAddressRequest request) {
    return ResponseEntity.ok(addressService.updateAddress(customerId, addressId, request));
  }

  @PutMapping("/{addressId}/default")
  public ResponseEntity<AddressResponse> setDefaultAddress(
      @RequestParam Long customerId, @PathVariable Long addressId) {
    return ResponseEntity.ok(addressService.setDefaultAddress(customerId, addressId));
  }
}
