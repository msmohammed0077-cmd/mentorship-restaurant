package com.mentorship.restaurant.order.controller;

import com.mentorship.restaurant.order.exception.TransitionNotAllowedForRoleException;
import com.mentorship.restaurant.order.model.entity.ActorRole;
import com.mentorship.restaurant.order.model.request.AcceptOrderRequest;
import com.mentorship.restaurant.order.model.request.RejectOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderStatusResponse;
import com.mentorship.restaurant.order.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order status")
@RequiredArgsConstructor
public class OrderStatusController {
  private final OrderService orderService;

  private void refuseSystemRole(ActorRole role) {
    if (role == ActorRole.SYSTEM) {
      throw new TransitionNotAllowedForRoleException("Role SYSTEM may not be supplied by a caller");
    }
  }

  @PostMapping("/{orderId}/accept")
  public ResponseEntity<OrderStatusResponse> accept(
      @PathVariable Long orderId,
      @RequestParam Long restaurantId,
      @RequestParam ActorRole role,
      @Valid @RequestBody(required = false) AcceptOrderRequest request) {
    refuseSystemRole(role);
    Integer prepTimeMinutes = request == null ? null : request.getPrepTimeMinutes();
    return ResponseEntity.ok(orderService.accept(orderId, restaurantId, role, prepTimeMinutes));
  }

  @PostMapping("/{orderId}/reject")
  public ResponseEntity<OrderStatusResponse> reject(
      @PathVariable Long orderId,
      @RequestParam Long restaurantId,
      @RequestParam ActorRole role,
      @Valid @RequestBody RejectOrderRequest request) {
    refuseSystemRole(role);
    return ResponseEntity.ok(
        orderService.reject(orderId, restaurantId, role, request.getReason(), request.getNote()));
  }

  @PostMapping("/{orderId}/preparing")
  public ResponseEntity<OrderStatusResponse> startPreparing(
      @PathVariable Long orderId, @RequestParam Long restaurantId, @RequestParam ActorRole role) {
    refuseSystemRole(role);
    return ResponseEntity.ok(orderService.startPreparing(orderId, restaurantId, role));
  }

  @PostMapping("/{orderId}/ready-for-pickup")
  public ResponseEntity<OrderStatusResponse> readyForPickup(
      @PathVariable Long orderId, @RequestParam Long restaurantId, @RequestParam ActorRole role) {
    refuseSystemRole(role);
    return ResponseEntity.ok(orderService.readyForPickup(orderId, restaurantId, role));
  }
}
