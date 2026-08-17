package com.mentorship.restaurant.cart.application.command;

public record UpdateCartItemQuantityCommand(Long cartItemId, Integer quantity) {
}
