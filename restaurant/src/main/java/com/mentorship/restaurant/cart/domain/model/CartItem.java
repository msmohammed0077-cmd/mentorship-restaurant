package com.mentorship.restaurant.cart.domain.model;

import java.math.BigDecimal;

public record CartItem(Long id, Long menuItemId, String itemName, BigDecimal itemPrice, Integer quantity) {
}
