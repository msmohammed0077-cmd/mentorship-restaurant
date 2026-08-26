package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.exception.CartItemNotFoundException;
import com.mentorship.restaurant.cart.exception.InvalidQuantityException;
import com.mentorship.restaurant.cart.exception.OutOfStockException;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.cart.model.mapper.CartMapper;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModifyCartItemHandler {

    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;

    public ModifyCartItemHandler(
            CartItemRepository cartItemRepository,
            CartMapper cartMapper
    ) {
        this.cartItemRepository = cartItemRepository;
        this.cartMapper = cartMapper;
    }

    @Transactional
    public CartResponse modifyItem(Long cartId, Long cartItemId, Integer quantity, String note) {
        validateQuantity(quantity);

        CartItem cartItem = cartItemRepository.findByIdAndCart_Id(cartItemId, cartId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found"));

        ensureStockAvailable(quantity, cartItem.getMenuItem().getStock());

        cartItem.setQuantity(quantity);
        cartItem.setNote(note);
        cartItem.setItemPrice(cartItem.getMenuItem().getItemPrice());

        return cartMapper.toResponse(cartItem.getCart());
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidQuantityException("Quantity must be greater than zero");
        }
    }

    private void ensureStockAvailable(Integer requestedQuantity, Integer availableStock) {
        if (availableStock == null || requestedQuantity > availableStock) {
            throw new OutOfStockException("Requested quantity exceeds available stock");
        }
    }
}
