package com.mentorship.restaurant.cart.domain.model;

import java.util.List;

public record Cart(Long id, Long customerId, List<CartItem> items) {
}
