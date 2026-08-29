package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.exception.CartItemNotFoundException;
import com.mentorship.restaurant.cart.exception.CartNotFoundException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.cart.model.mapper.CartMapper;
import com.mentorship.restaurant.cart.repository.CartRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemoveCartItemHandler {

  private final CartRepository cartRepository;
  private final CartMapper cartMapper;

  public RemoveCartItemHandler(CartRepository cartRepository, CartMapper cartMapper) {
    this.cartRepository = cartRepository;
    this.cartMapper = cartMapper;
  }

  @Transactional
  public CartResponse removeCartItems(Long cartId, List<Long> cartItemIds) {
    Cart cart =
        cartRepository
            .findById(cartId)
            .orElseThrow(() -> new CartNotFoundException("Cart not found"));

    List<CartItem> items = cart.getItems();
    removeCartItems(cartItemIds, items);

    if (items.isEmpty()) {
      cartRepository.delete(cart);
      return null;
    }

    Cart savedCart = cartRepository.save(cart);
    return cartMapper.toResponse(savedCart);
  }

  public void removeCartItems(List<Long> cartItemIds, List<CartItem> items) {
    Set<Long> existingIds = items.stream().map(CartItem::getId).collect(Collectors.toSet());

    List<Long> missingIds = cartItemIds.stream().filter(id -> !existingIds.contains(id)).toList();

    if (!missingIds.isEmpty()) {
      throw new CartItemNotFoundException("Cart items not found: " + missingIds);
    }

    items.removeIf(item -> cartItemIds.contains(item.getId()));
  }
}
