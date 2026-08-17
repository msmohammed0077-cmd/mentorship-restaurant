package com.mentorship.restaurant.cart.presentation.rest.request;

public record AddCartItemRequest(Long customerId, Long menuItemId, Integer quantity) {
}
