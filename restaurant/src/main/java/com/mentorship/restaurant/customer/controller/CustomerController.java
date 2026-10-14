package com.mentorship.restaurant.customer.controller;

import com.mentorship.restaurant.customer.model.request.ChangePasswordRequest;
import com.mentorship.restaurant.customer.model.request.CreateCustomerRequest;
import com.mentorship.restaurant.customer.model.request.UpdateCustomerRequest;
import com.mentorship.restaurant.customer.model.response.CustomerResponse;
import com.mentorship.restaurant.customer.service.CustomerDeletionService;
import com.mentorship.restaurant.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

  @PostMapping
  public ResponseEntity<CustomerResponse> createCustomer(
      @Valid @RequestBody CreateCustomerRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
  }

  @PreAuthorize("hasAuthority('customer.read')")
  @GetMapping("/{customerId}")
  public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long customerId) {
    return ResponseEntity.ok(customerService.getCustomer(customerId));
  }

  @PreAuthorize("hasAuthority('customer.write')")
  @PatchMapping("/{customerId}")
  public ResponseEntity<CustomerResponse> updateCustomer(
      @PathVariable Long customerId, @Valid @RequestBody UpdateCustomerRequest request) {
    return ResponseEntity.ok(customerService.updateCustomer(customerId, request));
  }

  @PreAuthorize("hasAuthority('customer.write')")
  @PutMapping("/{customerId}/password")
  public ResponseEntity<Void> changePassword(
      @PathVariable Long customerId, @Valid @RequestBody ChangePasswordRequest request) {
    customerService.changePassword(customerId, request);
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAuthority('customer.write')")
  @DeleteMapping("/{customerId}")
  public ResponseEntity<Void> deleteCustomer(@PathVariable Long customerId) {
    customerDeletionService.deleteCustomer(customerId);
    return ResponseEntity.noContent().build();
  }
}
