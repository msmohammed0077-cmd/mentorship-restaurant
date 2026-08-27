package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.exception.CartItemNotFoundException;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.cart.model.mapper.CartMapper;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ViewCartHandler {
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;

    public ViewCartHandler(CartItemRepository cartItemRepository, CartMapper cartMapper) {
        this.cartItemRepository = cartItemRepository;
        this.cartMapper = cartMapper;
    }


    public CartResponse viewCart(Long cartId) {
        List<CartItem> cartItemsList =
               cartItemRepository
                       .findAllByCart_Id(cartId)
                       .orElseThrow(() -> new CartItemNotFoundException("Cart not found"));

        if (cartItemsList.isEmpty()) {
            return cartMapper.toResponse(cartId);
        }

        cartItemsList.forEach(cartItem -> {
            if (cartItem.getMenuItem().getStock() == null || cartItem.getQuantity() > cartItem.getMenuItem().getStock()) {
                throw new CartItemNotFoundException("Cart item with ID " + cartItem.getId() + " exceeds available stock");
            }
        });

         return cartMapper.toResponse(cartItemsList.get(0).getCart());
    }
}
