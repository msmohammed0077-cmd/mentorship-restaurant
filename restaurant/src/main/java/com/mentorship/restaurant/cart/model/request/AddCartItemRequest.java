package com.mentorship.restaurant.cart.model.request;

public record AddCartItemRequest(Long customerId, Long menuItemId, Integer quantity) {
}
