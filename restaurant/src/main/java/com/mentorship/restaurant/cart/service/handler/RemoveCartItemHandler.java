package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.exception.CartItemNotFoundException;
import com.mentorship.restaurant.cart.exception.CartNotFoundException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.mapper.CartMapper;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import com.mentorship.restaurant.cart.repository.CartRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemoveCartItemHandler {

  private final CartItemRepository cartItemRepository;
  private final CartRepository cartRepository;
  private final CartMapper cartMapper;

  public RemoveCartItemHandler(
      CartItemRepository cartItemRepository, CartRepository cartRepository, CartMapper cartMapper) {
    this.cartItemRepository = cartItemRepository;
    this.cartRepository = cartRepository;
    this.cartMapper = cartMapper;
  }

  /**
   * Removing the last item empties the cart but does not delete it. A customer who removes
   * everything still has a cart, which is what clearCart already returns and what a following GET
   * expects to find.
   *
   * <p>The cart is read only after the delete: see ClearCartHandler for why loading it first would
   * leave a stale item collection behind.
   */
  @Transactional
  public CartResponse removeCartItems(Long cartId, List<Long> cartItemIds) {
    // Before the delete, so a missing cart is reported as such rather than as items not found.
    ensureCartExists(cartId);

    // The same id twice would delete one row, which would otherwise read as a missing item.
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

  /**
   * Checked before the delete rather than inferred from its row count afterwards. The count says
   * how many ids missed but not which, and by the time it is known the rows that did match are
   * already gone, so there is nothing left to compare against.
   *
   * <p>Scoping the lookup by cart is what stops one cart removing another's items.
   */
  private void ensureAllItemsInCart(Long cartId, Set<Long> requestedIds) {
    List<Long> present = cartItemRepository.findIdsByCart_IdAndIdIn(cartId, requestedIds);
    if (present.size() == requestedIds.size()) {
      return;
    }
    List<Long> missing = requestedIds.stream().filter(id -> !present.contains(id)).toList();
    throw new CartItemNotFoundException("Cart items not found: " + missing);
  }
}
