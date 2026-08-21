package com.mentorship.restaurant.cart.service.handler;

public record AddToCartHandler(Long customerId, Long menuItemId, Integer quantity) {
}
