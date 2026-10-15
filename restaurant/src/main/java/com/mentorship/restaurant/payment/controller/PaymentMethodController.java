package com.mentorship.restaurant.payment.controller;

import com.mentorship.restaurant.payment.model.request.AddPaymentMethodRequest;
import com.mentorship.restaurant.payment.model.response.PaymentMethodResponse;
import com.mentorship.restaurant.payment.service.PaymentMethodService;
import com.mentorship.restaurant.user.model.Caller;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payment-methods")
@Tag(name = "Payment Methods")
@RequiredArgsConstructor
public class PaymentMethodController {

  private final PaymentMethodService paymentMethodService;

  @PreAuthorize("hasAuthority('payment-method.write')")
  @PostMapping
  public ResponseEntity<PaymentMethodResponse> addPaymentMethod(
      @AuthenticationPrincipal Caller caller, @Valid @RequestBody AddPaymentMethodRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(paymentMethodService.addPaymentMethod(caller.requireCustomerId(), request));
  }

  @PreAuthorize("hasAuthority('payment-method.read')")
  @GetMapping
  public ResponseEntity<List<PaymentMethodResponse>> viewPaymentMethods(
      @AuthenticationPrincipal Caller caller) {
    return ResponseEntity.ok(paymentMethodService.viewPaymentMethods(caller.requireCustomerId()));
  }

  @PreAuthorize("hasAuthority('payment-method.write')")
  @PutMapping("/{paymentMethodId}/default")
  public ResponseEntity<PaymentMethodResponse> setDefaultPaymentMethod(
      @AuthenticationPrincipal Caller caller, @PathVariable Long paymentMethodId) {
    return ResponseEntity.ok(
        paymentMethodService.setDefaultPaymentMethod(caller.requireCustomerId(), paymentMethodId));
  }

  @PreAuthorize("hasAuthority('payment-method.write')")
  @DeleteMapping("/{paymentMethodId}")
  public ResponseEntity<Void> deletePaymentMethod(
      @AuthenticationPrincipal Caller caller, @PathVariable Long paymentMethodId) {
    paymentMethodService.deletePaymentMethod(caller.requireCustomerId(), paymentMethodId);
    return ResponseEntity.noContent().build();
  }
}
