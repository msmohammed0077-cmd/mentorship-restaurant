package com.mentorship.restaurant.cart.service;

import com.mentorship.restaurant.cart.service.handler.AddToCartHandler;
import com.mentorship.restaurant.cart.service.handler.CheckoutCartHandler;
import com.mentorship.restaurant.cart.service.handler.ClearCartHandler;
import com.mentorship.restaurant.cart.service.handler.RemoveCartItemHandler;
import com.mentorship.restaurant.cart.service.handler.UpdateCartItemQuantityHandler;
import com.mentorship.restaurant.cart.service.handler.ViewCartHandler;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import com.mentorship.restaurant.cart.repository.CartRepository;
import com.mentorship.restaurant.cart.repository.CustomerRepository;
import com.mentorship.restaurant.cart.repository.MenuItemRepository;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final MenuItemRepository menuItemRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            CustomerRepository customerRepository,
            MenuItemRepository menuItemRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.customerRepository = customerRepository;
        this.menuItemRepository = menuItemRepository;
    }

    public void addItem(Long customerId, Long menuItemId, Integer quantity) {
        AddToCartHandler command = new AddToCartHandler(customerId, menuItemId, quantity);
        throw new UnsupportedOperationException("Add to cart is not implemented yet");
    }

    public void updateQuantity(Long cartItemId, Integer quantity) {
        UpdateCartItemQuantityHandler command = new UpdateCartItemQuantityHandler(cartItemId, quantity);
        throw new UnsupportedOperationException("Update quantities is not implemented yet");
    }

    public void removeItem(Long cartItemId) {
        RemoveCartItemHandler command = new RemoveCartItemHandler(cartItemId);
        throw new UnsupportedOperationException("Remove item from cart is not implemented yet");
    }

    public void viewCart(Long cartId) {
        ViewCartHandler query = new ViewCartHandler(cartId);
        throw new UnsupportedOperationException("View cart is not implemented yet");
    }

    public void clearCart(Long cartId) {
        ClearCartHandler command = new ClearCartHandler(cartId);
        throw new UnsupportedOperationException("Clear cart is not implemented yet");
    }

    public void checkout(Long cartId) {
        CheckoutCartHandler command = new CheckoutCartHandler(cartId);
        throw new UnsupportedOperationException("Checkout is not implemented yet");
    }
}
