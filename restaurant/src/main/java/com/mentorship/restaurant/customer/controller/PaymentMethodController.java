package com.mentorship.restaurant.customer.controller;

import com.mentorship.restaurant.customer.model.request.AddPaymentMethodRequest;
import com.mentorship.restaurant.customer.model.response.PaymentMethodResponse;
import com.mentorship.restaurant.customer.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payment-methods")
@Tag(name = "Payment Methods")
@RequiredArgsConstructor
public class PaymentMethodController {

  private final PaymentMethodService paymentMethodService;

  @PostMapping
  public ResponseEntity<PaymentMethodResponse> addPaymentMethod(
      @RequestParam Long customerId, @Valid @RequestBody AddPaymentMethodRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(paymentMethodService.addPaymentMethod(customerId, request));
  }

  @GetMapping
  public ResponseEntity<List<PaymentMethodResponse>> viewPaymentMethods(
      @RequestParam Long customerId) {
    return ResponseEntity.ok(paymentMethodService.viewPaymentMethods(customerId));
  }

  @PutMapping("/{paymentMethodId}/default")
  public ResponseEntity<PaymentMethodResponse> setDefaultPaymentMethod(
      @RequestParam Long customerId, @PathVariable Long paymentMethodId) {
    return ResponseEntity.ok(
        paymentMethodService.setDefaultPaymentMethod(customerId, paymentMethodId));
  }

  @DeleteMapping("/{paymentMethodId}")
  public ResponseEntity<Void> deletePaymentMethod(
      @RequestParam Long customerId, @PathVariable Long paymentMethodId) {
    paymentMethodService.deletePaymentMethod(customerId, paymentMethodId);
    return ResponseEntity.noContent().build();
  }
}
