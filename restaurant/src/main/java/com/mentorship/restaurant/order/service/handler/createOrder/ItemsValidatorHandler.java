package com.mentorship.restaurant.order.service.handler.createOrder;

import com.mentorship.restaurant.cart.exception.EmptyCartException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.cart.repository.MenuItemRepository;
import com.mentorship.restaurant.order.exception.OrderQuantityIsNotAllowed;
import com.mentorship.restaurant.order.model.request.CreateOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderResponse;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class ItemsValidatorHandler extends OrderHandler {

    private final Cart cart;

    @Override
    public OrderResponse handle(CreateOrderRequest request, OrderResponse response) {
        List<CartItem> cartItems = cart.getItems();

        if (cartItems.isEmpty()) {
            throw new EmptyCartException("Order empty cart is not allowed");
        }

        for (CartItem cartItem : cartItems) {
            Integer orderedQuantity = cartItem.getQuantity();
            Integer stockQuantity = cartItem.getMenuItem().getStock();

            if (orderedQuantity > stockQuantity) {
                throw new OrderQuantityIsNotAllowed("Order Quantity is higher than available stock");
            }
        }

        return handleNext(request, response);
    }
}
