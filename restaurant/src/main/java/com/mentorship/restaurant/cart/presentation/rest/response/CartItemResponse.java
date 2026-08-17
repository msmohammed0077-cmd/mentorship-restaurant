package com.mentorship.restaurant.cart.presentation.rest.response;

import java.math.BigDecimal;

public record CartItemResponse(Long id, Long menuItemId, String itemName, BigDecimal itemPrice, Integer quantity) {
}
