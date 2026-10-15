package com.mentorship.restaurant.order.controller;

import com.mentorship.restaurant.order.model.request.CreateOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderResponse;
import com.mentorship.restaurant.order.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.mentorship.restaurant.user.model.Caller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  @PreAuthorize("hasAuthority('order.create')")
  @PostMapping()
  public ResponseEntity<OrderResponse> createOrder(
      @AuthenticationPrincipal Caller caller, @Valid @RequestBody CreateOrderRequest request) {
    OrderResponse response = orderService.createOrder(caller.requireCustomerId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PreAuthorize("hasAuthority('order.read')")
  @GetMapping("/{orderId}")
  public ResponseEntity<OrderResponse> viewOrderDetails(
      @AuthenticationPrincipal Caller caller, @PathVariable Long orderId) {
    // Customers and restaurant owners both read orders, so a null customer id is fine here.
    return ResponseEntity.ok(
        orderService.viewOrderDetails(orderId, caller.customerId(), caller.userId()));
  }
}
