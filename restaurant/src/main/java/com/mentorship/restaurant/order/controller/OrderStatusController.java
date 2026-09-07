package com.mentorship.restaurant.order.controller;

import com.mentorship.restaurant.order.model.entity.ActorRole;
import com.mentorship.restaurant.order.model.response.OrderStatusResponse;
import com.mentorship.restaurant.order.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A verb per transition, not one generic PATCH /status: accept carries a prep time, reject carries
 * a reason, and each authorizes differently.
 *
 * <p>restaurantId and role are scoping, not authorisation — any caller may pass any value. Accept
 * and reject are GH-48's; cancel is GH-41's.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order status")
@RequiredArgsConstructor
public class OrderStatusController {

  private final OrderService orderService;

  @PostMapping("/{orderId}/preparing")
  public ResponseEntity<OrderStatusResponse> startPreparing(
      @PathVariable Long orderId, @RequestParam Long restaurantId, @RequestParam ActorRole role) {
    return ResponseEntity.ok(orderService.startPreparing(orderId, restaurantId, role));
  }

  @PostMapping("/{orderId}/ready-for-pickup")
  public ResponseEntity<OrderStatusResponse> readyForPickup(
      @PathVariable Long orderId, @RequestParam Long restaurantId, @RequestParam ActorRole role) {
    return ResponseEntity.ok(orderService.readyForPickup(orderId, restaurantId, role));
  }
}
