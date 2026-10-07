package com.mentorship.restaurant.order.controller;

import com.mentorship.restaurant.order.model.request.RateOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderRatingResponse;
import com.mentorship.restaurant.order.service.OrderRatingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

  @PostMapping("/{orderId}/rating")
  public ResponseEntity<OrderRatingResponse> rate(
      @PathVariable Long orderId, @Valid @RequestBody RateOrderRequest request) {
    OrderRatingResponse response =
        orderRatingService.rate(
            orderId, request.getCustomerId(), request.getScore(), request.getComment());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
