package com.mentorship.restaurant.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * A thin trigger. The logic lives in AutoRejectStaleOrdersHandler so tests can invoke it directly
 * rather than waiting on a timer.
 *
 * <p>Switchable because a background sweep and a test suite that seeds PLACED orders with past
 * timestamps are in a race the suite would eventually lose. Tests disable the timer and call the
 * handler themselves, which tests the same code without the timing.
 */
@Component
@ConditionalOnProperty(
    name = "app.orders.auto-reject.enabled",
    havingValue = "true",
    matchIfMissing = true)
@RequiredArgsConstructor
public class AutoRejectStaleOrdersJob {

  private final OrderService orderService;

  @Scheduled(fixedDelayString = "PT1M")
  public void sweep() {
    orderService.autoRejectStaleOrders();
  }
}
