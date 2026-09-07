package com.mentorship.restaurant.order.service.handler;

import com.mentorship.restaurant.cart.repository.MenuItemRepository;
import com.mentorship.restaurant.order.repository.OrderItemRepository;
import com.mentorship.restaurant.order.repository.OrderLineProjection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Undoes what a placed order consumed. Reject (GH-48) and cancel (GH-41) need this identically:
 * cancel is only legal while PLACED and reject only happens from PLACED, so they are the same
 * operation from the same state. Written once — two copies would drift, and the copy that drifts is
 * the one that silently fails to restore stock.
 *
 * <p><b>Refund is not implemented.</b> There is no payment in this codebase; checkout's "Payment
 * successful" is a hardcoded string. When payment is real it belongs in this method.
 *
 * <p><b>Coupon release is not implemented.</b> There are no coupons in this codebase — no table, no
 * entity, no endpoint. When they exist they belong in this method.
 *
 * <p>Both are named here rather than silently absent, so the next person adds them in one place.
 */
@Service
@RequiredArgsConstructor
public class CompensateOrderHandler {

  private final OrderItemRepository orderItemRepository;
  private final MenuItemRepository menuItemRepository;

  /**
   * MANDATORY is deliberate: compensation must never run in a transaction of its own. If the
   * caller's transaction rolls back, the restock has to roll back with it, or a failed rejection
   * hands stock back anyway.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void compensate(Long orderId) {
    List<OrderLineProjection> lines = orderItemRepository.findLinesByOrderId(orderId);
    lines.forEach(
        line -> menuItemRepository.incrementStock(line.getMenuItemId(), line.getQuantity()));
  }
}
