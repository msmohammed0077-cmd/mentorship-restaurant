package com.mentorship.restaurant.cart.service;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.service.handler.ModifyCartItemHandler;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    private final ModifyCartItemHandler modifyCartItemHandler;

    public CartService(ModifyCartItemHandler modifyCartItemHandler) {
        this.modifyCartItemHandler = modifyCartItemHandler;
    }

    public CartResponse modifyItem(Long cartId, Long cartItemId, Integer quantity, String note) {
        return modifyCartItemHandler.modifyItem(cartId, cartItemId, quantity, note);
    }
}
