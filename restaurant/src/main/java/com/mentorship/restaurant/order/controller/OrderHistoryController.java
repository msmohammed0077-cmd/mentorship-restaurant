package com.mentorship.restaurant.order.controller;

import com.mentorship.restaurant.order.model.request.ViewOrderHistoryRequest;
import com.mentorship.restaurant.order.model.response.OrderHistoryResponse;
import com.mentorship.restaurant.order.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Separate from OrderStatusController: that one owns transitions, this one owns a read.
 *
 * <p>customerId is scoping, not authorisation — any caller may pass any id until auth lands.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order history")
@RequiredArgsConstructor
public class OrderHistoryController {

  private final OrderService orderService;

  @GetMapping
  public ResponseEntity<OrderHistoryResponse> viewOrderHistory(
      @Valid @ModelAttribute ViewOrderHistoryRequest request) {
    return ResponseEntity.ok(
        orderService.viewOrderHistory(
            request.getCustomerId(), request.getLimit(), request.getCursor()));
  }
}
