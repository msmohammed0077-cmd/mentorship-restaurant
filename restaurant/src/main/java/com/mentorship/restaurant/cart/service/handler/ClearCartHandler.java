package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.cart.model.mapper.CartMapper;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClearCartHandler {
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;

    public ClearCartHandler(CartItemRepository cartItemRepository, CartMapper cartMapper) {
        this.cartItemRepository = cartItemRepository;
        this.cartMapper = cartMapper;
    }

    @Transactional
    public CartResponse clearCart(Long cartId) {
        List<CartItem> cartItems =
            cartItemRepository.findAllByCart_Id(cartId).orElse(Collections.emptyList());

        if (cartItems.isEmpty()) {
            return cartMapper.toResponse(cartId);
        }

        cartItemRepository.deleteAll(cartItems);
        return cartMapper.toResponse(cartItems.get(0).getCart());
    }
}
