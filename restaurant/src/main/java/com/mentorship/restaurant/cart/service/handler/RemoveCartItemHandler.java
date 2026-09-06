package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.exception.CartItemNotFoundException;
import com.mentorship.restaurant.cart.exception.CartNotFoundException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.mapper.CartMapper;
import com.mentorship.restaurant.cart.model.response.CartResponse;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import com.mentorship.restaurant.cart.repository.CartRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RemoveCartItemHandler {

  private final CartItemRepository cartItemRepository;
  private final CartRepository cartRepository;
  private final CartMapper cartMapper;

  @Transactional
  public CartResponse removeCartItems(Long cartId, List<Long> cartItemIds) {
    ensureCartExists(cartId);

    Set<Long> requestedIds = new LinkedHashSet<>(cartItemIds);
    ensureAllItemsInCart(cartId, requestedIds);

    cartItemRepository.deleteAllByCart_IdAndIdIn(cartId, requestedIds);

    Cart cart =
        cartRepository
            .findById(cartId)
            .orElseThrow(() -> new CartNotFoundException("Cart not found"));

    return cartMapper.toResponse(cart);
  }

  private void ensureCartExists(Long cartId) {
    if (!cartRepository.existsById(cartId)) {
      throw new CartNotFoundException("Cart not found");
    }
  }

  private void ensureAllItemsInCart(Long cartId, Set<Long> requestedIds) {
    List<Long> present = cartItemRepository.findIdsByCart_IdAndIdIn(cartId, requestedIds);
    if (present.size() == requestedIds.size()) {
      return;
    }
    List<Long> missing = requestedIds.stream().filter(id -> !present.contains(id)).toList();
    throw new CartItemNotFoundException("Cart items not found: " + missing);
  }
}
