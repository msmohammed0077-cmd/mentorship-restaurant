package com.mentorship.restaurant.order.controller;

import com.mentorship.restaurant.order.model.entity.ActorRole;
import com.mentorship.restaurant.order.model.request.AcceptOrderRequest;
import com.mentorship.restaurant.order.model.request.RejectOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderStatusResponse;
import com.mentorship.restaurant.order.service.OrderStatusService;
import com.mentorship.restaurant.user.model.Caller;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
  private final OrderStatusService orderStatusService;

  @PreAuthorize("hasAuthority('order.accept')")
  @PostMapping("/{orderId}/accept")
  public ResponseEntity<OrderStatusResponse> accept(
      @AuthenticationPrincipal Caller caller,
      @PathVariable Long orderId,
      @RequestParam Long restaurantId,
      @Valid @RequestBody(required = false) AcceptOrderRequest request) {
    Integer prepTimeMinutes = request == null ? null : request.getPrepTimeMinutes();
    return ResponseEntity.ok(
        orderStatusService.accept(
            orderId, restaurantId, caller.userId(), ActorRole.RESTAURANT, prepTimeMinutes));
  }

  @PreAuthorize("hasAuthority('order.reject')")
  @PostMapping("/{orderId}/reject")
  public ResponseEntity<OrderStatusResponse> reject(
      @AuthenticationPrincipal Caller caller,
      @PathVariable Long orderId,
      @RequestParam Long restaurantId,
      @Valid @RequestBody RejectOrderRequest request) {
    return ResponseEntity.ok(
        orderStatusService.reject(
            orderId,
            restaurantId,
            caller.userId(),
            ActorRole.RESTAURANT,
            request.getReason(),
            request.getNote()));
  }

  @PreAuthorize("hasAuthority('order.cancel')")
  @PostMapping("/{orderId}/cancel")
  public ResponseEntity<OrderStatusResponse> cancel(
      @AuthenticationPrincipal Caller caller, @PathVariable Long orderId) {
    return ResponseEntity.ok(
        orderStatusService.cancel(orderId, caller.requireCustomerId(), ActorRole.CUSTOMER));
  }

  @PreAuthorize("hasAuthority('order.prepare')")
  @PostMapping("/{orderId}/preparing")
  public ResponseEntity<OrderStatusResponse> startPreparing(
      @AuthenticationPrincipal Caller caller,
      @PathVariable Long orderId,
      @RequestParam Long restaurantId) {
    return ResponseEntity.ok(
        orderStatusService.startPreparing(
            orderId, restaurantId, caller.userId(), ActorRole.RESTAURANT));
  }

  @PreAuthorize("hasAuthority('order.prepare')")
  @PostMapping("/{orderId}/ready-for-pickup")
  public ResponseEntity<OrderStatusResponse> readyForPickup(
      @AuthenticationPrincipal Caller caller,
      @PathVariable Long orderId,
      @RequestParam Long restaurantId) {
    return ResponseEntity.ok(
        orderStatusService.readyForPickup(
            orderId, restaurantId, caller.userId(), ActorRole.RESTAURANT));
  }
}
