package com.mentorship.restaurant.cart.domain.model;

import java.math.BigDecimal;

public record Checkout(Long cartId, BigDecimal total) {
}
