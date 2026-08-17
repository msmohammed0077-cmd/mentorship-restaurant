package com.mentorship.restaurant.cart.application.command;

public record AddToCartCommand(Long customerId, Long menuItemId, Integer quantity) {
}
