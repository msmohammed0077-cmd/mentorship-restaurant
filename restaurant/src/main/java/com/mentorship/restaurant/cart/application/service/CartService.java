package com.mentorship.restaurant.cart.application.service;

import com.mentorship.restaurant.cart.application.command.AddToCartCommand;
import com.mentorship.restaurant.cart.application.command.CheckoutCartCommand;
import com.mentorship.restaurant.cart.application.command.ClearCartCommand;
import com.mentorship.restaurant.cart.application.command.RemoveCartItemCommand;
import com.mentorship.restaurant.cart.application.command.UpdateCartItemQuantityCommand;
import com.mentorship.restaurant.cart.application.query.ViewCartQuery;
import com.mentorship.restaurant.cart.domain.model.Cart;
import com.mentorship.restaurant.cart.domain.model.Checkout;
import com.mentorship.restaurant.cart.infrastructure.persistence.repository.CartItemRepository;
import com.mentorship.restaurant.cart.infrastructure.persistence.repository.CartRepository;
import com.mentorship.restaurant.cart.infrastructure.persistence.repository.CustomerRepository;
import com.mentorship.restaurant.cart.infrastructure.persistence.repository.MenuItemRepository;
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

    public Cart addItem(Long customerId, Long menuItemId, Integer quantity) {
        AddToCartCommand command = new AddToCartCommand(customerId, menuItemId, quantity);
        throw new UnsupportedOperationException("Add to cart is not implemented yet");
    }

    public Cart updateQuantity(Long cartItemId, Integer quantity) {
        UpdateCartItemQuantityCommand command = new UpdateCartItemQuantityCommand(cartItemId, quantity);
        throw new UnsupportedOperationException("Update quantities is not implemented yet");
    }

    public void removeItem(Long cartItemId) {
        RemoveCartItemCommand command = new RemoveCartItemCommand(cartItemId);
        throw new UnsupportedOperationException("Remove item from cart is not implemented yet");
    }

    public Cart viewCart(Long cartId) {
        ViewCartQuery query = new ViewCartQuery(cartId);
        throw new UnsupportedOperationException("View cart is not implemented yet");
    }

    public void clearCart(Long cartId) {
        ClearCartCommand command = new ClearCartCommand(cartId);
        throw new UnsupportedOperationException("Clear cart is not implemented yet");
    }

    public Checkout checkout(Long cartId) {
        CheckoutCartCommand command = new CheckoutCartCommand(cartId);
        throw new UnsupportedOperationException("Checkout is not implemented yet");
    }
}
