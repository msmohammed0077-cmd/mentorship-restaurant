package com.mentorship.restaurant.cart.controller;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.model.request.UpdateCartItemRequest;
import com.mentorship.restaurant.cart.service.CartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart")
public class CartController {

  private final CartService cartService;

  public CartController(CartService cartService) {
    this.cartService = cartService;
  }

  @PutMapping("/{cartId}/items/{cartItemId}")
  public ResponseEntity<CartResponse> modifyItem(
      @PathVariable Long cartId,
      @PathVariable Long cartItemId,
      @Valid @RequestBody UpdateCartItemRequest request) {
    return ResponseEntity.ok(
        cartService.modifyItem(cartId, cartItemId, request.getQuantity(), request.getNote()));
  }
}
