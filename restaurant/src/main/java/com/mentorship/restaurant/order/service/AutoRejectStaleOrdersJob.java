package com.mentorship.restaurant.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "app.orders.auto-reject.enabled",
    havingValue = "true",
    matchIfMissing = true)
@RequiredArgsConstructor
public class AutoRejectStaleOrdersJob {
  private final OrderStatusService orderStatusService;

  @Scheduled(fixedDelayString = "PT1M")
  public void sweep() {
    orderStatusService.autoRejectStaleOrders();
  }
}
