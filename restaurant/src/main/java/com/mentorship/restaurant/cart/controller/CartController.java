package com.mentorship.restaurant.cart.controller;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.model.request.UpdateCartItemRequest;
import com.mentorship.restaurant.cart.service.CartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart")
public class CartController {

  private final CartService cartService;

  public CartController(CartService cartService) {
    this.cartService = cartService;
  }

  @PutMapping("/{cartId}/items/{cartItemId}")
  @Transactional
  public ResponseEntity<CartResponse> modifyItem(
      @PathVariable Long cartId,
      @PathVariable Long cartItemId,
      @Valid @RequestBody UpdateCartItemRequest request) {
    return ResponseEntity.ok(
        cartService.modifyItem(cartId, cartItemId, request.getQuantity(), request.getNote()));
  }

  @GetMapping("/{cartId}")
  @Transactional(readOnly = true)
  public ResponseEntity<CartResponse> viewCart(@PathVariable Long cartId) {
    return ResponseEntity.ok(cartService.viewCart(cartId));
  }

  @DeleteMapping("/{cartId}")
  @Transactional
  public ResponseEntity<CartResponse> clearCart(@PathVariable Long cartId) {
    return ResponseEntity.ok(cartService.clearCart(cartId));
  }
}
