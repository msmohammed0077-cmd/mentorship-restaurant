package com.mentorship.restaurant.cart.presentation.rest.response;

import java.math.BigDecimal;

public record CheckoutResponse(Long cartId, BigDecimal total) {
}
