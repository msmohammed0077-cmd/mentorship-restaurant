package com.mentorship.restaurant.order.controller;

import com.mentorship.restaurant.order.model.request.RateOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderRatingResponse;
import com.mentorship.restaurant.order.service.OrderRatingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.mentorship.restaurant.user.model.Caller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order rating")
@RequiredArgsConstructor
public class OrderRatingController {
  private final OrderRatingService orderRatingService;

  @PreAuthorize("hasAuthority('order.rate')")
  @PostMapping("/{orderId}/rating")
  public ResponseEntity<OrderRatingResponse> rateOrder(
      @AuthenticationPrincipal Caller caller,
      @PathVariable Long orderId,
      @Valid @RequestBody RateOrderRequest request) {
    OrderRatingResponse response =
        orderRatingService.rateOrder(
            orderId, caller.requireCustomerId(), request.getScore(), request.getComment());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
