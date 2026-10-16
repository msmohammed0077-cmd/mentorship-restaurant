package com.mentorship.restaurant.customer.controller;

import com.mentorship.restaurant.config.PublicEndpoint;
import com.mentorship.restaurant.customer.model.request.ChangePasswordRequest;
import com.mentorship.restaurant.customer.model.request.CreateCustomerRequest;
import com.mentorship.restaurant.customer.model.request.UpdateCustomerRequest;
import com.mentorship.restaurant.customer.model.response.CustomerResponse;
import com.mentorship.restaurant.customer.service.CustomerDeletionService;
import com.mentorship.restaurant.customer.service.CustomerService;
import com.mentorship.restaurant.user.model.Caller;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers")
@RequiredArgsConstructor
public class CustomerController {

  private final CustomerService customerService;
  private final CustomerDeletionService customerDeletionService;

  // Registration: the caller has no account yet. #83 part 3 replaces it.
  @PublicEndpoint
  @SecurityRequirements
  @PostMapping
  public ResponseEntity<CustomerResponse> createCustomer(
      @Valid @RequestBody CreateCustomerRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
  }

  @PreAuthorize("hasAuthority('customer.read')")
  @GetMapping("/me")
  public ResponseEntity<CustomerResponse> getCustomer(@AuthenticationPrincipal Caller caller) {
    return ResponseEntity.ok(customerService.getCustomer(caller.requireCustomerId()));
  }

  @PreAuthorize("hasAuthority('customer.write')")
  @PatchMapping("/me")
  public ResponseEntity<CustomerResponse> updateCustomer(
      @AuthenticationPrincipal Caller caller, @Valid @RequestBody UpdateCustomerRequest request) {
    return ResponseEntity.ok(customerService.updateCustomer(caller.requireCustomerId(), request));
  }

  @PreAuthorize("hasAuthority('customer.write')")
  @PutMapping("/me/password")
  public ResponseEntity<Void> changePassword(
      @AuthenticationPrincipal Caller caller, @Valid @RequestBody ChangePasswordRequest request) {
    customerService.changePassword(caller.requireCustomerId(), request);
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAuthority('customer.write')")
  @DeleteMapping("/me")
  public ResponseEntity<Void> deleteCustomer(@AuthenticationPrincipal Caller caller) {
    customerDeletionService.deleteCustomer(caller.requireCustomerId());
    return ResponseEntity.noContent().build();
  }
}
