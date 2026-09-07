package com.mentorship.restaurant.order.service.handler;

import com.mentorship.restaurant.order.model.entity.ActorRole;
import com.mentorship.restaurant.order.model.entity.RejectionReason;
import com.mentorship.restaurant.order.repository.OrderRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Rejects every order left in PLACED past the deadline, as SYSTEM with NO_RESPONSE.
 *
 * <p>Not a separate "expired" status: GH-34 settled that expiry writes REJECTED so it shares one
 * compensation path. A second terminal status would mean a second compensation path, and the second
 * one is the one that rots.
 *
 * <p>Each order is rejected in its own transaction, so one failure does not abandon the rest. No
 * locking is needed — an order the restaurant moved first is simply not matched by the conditional
 * update.
 */
@Service
@RequiredArgsConstructor
public class AutoRejectStaleOrdersHandler {

  private static final Logger log = LoggerFactory.getLogger(AutoRejectStaleOrdersHandler.class);

  /** Matches GH-35's payment timeout. */
  private static final int AUTO_REJECT_AFTER_MINUTES = 15;

  private final OrderRepository orderRepository;
  private final RejectOrderHandler rejectOrderHandler;

  public void autoRejectStaleOrders() {
    OffsetDateTime deadline = OffsetDateTime.now().minusMinutes(AUTO_REJECT_AFTER_MINUTES);
    List<Long> staleOrderIds = orderRepository.findStalePlacedOrderIds(deadline);

    for (Long orderId : staleOrderIds) {
      try {
        rejectOrderHandler.reject(
            orderId, null, ActorRole.SYSTEM, RejectionReason.NO_RESPONSE, null);
      } catch (RuntimeException exception) {
        log.warn("Auto-reject skipped order {}", orderId, exception);
      }
    }
  }
}
