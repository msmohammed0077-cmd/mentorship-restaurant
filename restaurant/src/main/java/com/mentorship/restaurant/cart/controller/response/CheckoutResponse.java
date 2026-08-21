package com.mentorship.restaurant.cart.controller.response;

import java.math.BigDecimal;

public record CheckoutResponse(Long cartId, BigDecimal total) {
}
