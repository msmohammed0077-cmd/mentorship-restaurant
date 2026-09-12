package com.mentorship.restaurant.order.controller;

import com.mentorship.restaurant.order.model.request.CreateOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderResponse;
import com.mentorship.restaurant.order.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  @PostMapping()
  public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    OrderResponse response = orderService.createOrder(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{orderId}")
  public ResponseEntity<OrderResponse> viewOrderDetails(@PathVariable Long orderId) {
    return ResponseEntity.ok(orderService.viewOrderDetails(orderId));
  }
}
