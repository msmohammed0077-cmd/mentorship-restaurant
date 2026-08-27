package com.mentorship.restaurant.cart.controller;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.model.request.AddCartItemRequest;
import com.mentorship.restaurant.cart.model.request.UpdateCartItemRequest;
import com.mentorship.restaurant.cart.service.CartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

  /**
   * The cart is resolved from customerId rather than a path variable, because this endpoint creates
   * the cart when none exists and the caller has no cartId before the first add. customerId moves
   * to the authenticated principal once auth lands.
   */
  @PostMapping("/items")
  public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
    CartResponse response =
        cartService.addItem(
            request.getCustomerId(),
            request.getMenuItemId(),
            request.getQuantity(),
            request.getNote());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
