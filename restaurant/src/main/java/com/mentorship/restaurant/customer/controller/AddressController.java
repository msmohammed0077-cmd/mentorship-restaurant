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
import com.mentorship.restaurant.user.model.Caller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/addresses")
@Tag(name = "Addresses")
@RequiredArgsConstructor
public class AddressController {

  private final AddressService addressService;

  @PreAuthorize("hasAuthority('address.write')")
  @PostMapping
  public ResponseEntity<AddressResponse> addAddress(
      @AuthenticationPrincipal Caller caller, @Valid @RequestBody AddAddressRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(addressService.addAddress(caller.requireCustomerId(), request));
  }

  @PreAuthorize("hasAuthority('address.read')")
  @GetMapping
  public ResponseEntity<List<AddressResponse>> viewAddresses(@AuthenticationPrincipal Caller caller) {
    return ResponseEntity.ok(addressService.viewAddresses(caller.requireCustomerId()));
  }

  @PreAuthorize("hasAuthority('address.write')")
  @PutMapping("/{addressId}")
  public ResponseEntity<AddressResponse> updateAddress(
      @AuthenticationPrincipal Caller caller,
      @PathVariable Long addressId,
      @Valid @RequestBody UpdateAddressRequest request) {
    return ResponseEntity.ok(addressService.updateAddress(caller.requireCustomerId(), addressId, request));
  }

  @PreAuthorize("hasAuthority('address.write')")
  @DeleteMapping("/{addressId}")
  public ResponseEntity<Void> deleteAddress(
      @AuthenticationPrincipal Caller caller, @PathVariable Long addressId) {
    addressService.deleteAddress(caller.requireCustomerId(), addressId);
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAuthority('address.write')")
  @PutMapping("/{addressId}/default")
  public ResponseEntity<AddressResponse> setDefaultAddress(
      @AuthenticationPrincipal Caller caller, @PathVariable Long addressId) {
    return ResponseEntity.ok(addressService.setDefaultAddress(caller.requireCustomerId(), addressId));
  }
}
