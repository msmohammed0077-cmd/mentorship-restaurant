package com.mentorship.restaurant.customer.controller;

import com.mentorship.restaurant.customer.model.request.CreateCustomerRequest;
import com.mentorship.restaurant.customer.model.response.CustomerResponse;
import com.mentorship.restaurant.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers")
@RequiredArgsConstructor
public class CustomerController {

  private final CustomerService customerService;

  @PostMapping
  public ResponseEntity<CustomerResponse> createCustomer(
      @Valid @RequestBody CreateCustomerRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
  }
}
