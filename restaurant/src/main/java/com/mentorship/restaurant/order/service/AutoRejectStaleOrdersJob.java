package com.mentorship.restaurant.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * A thin trigger. The logic lives in OrderService so tests can invoke it directly rather than
 * waiting on a timer.
 */
@Component
@RequiredArgsConstructor
public class AutoRejectStaleOrdersJob {

  private final OrderService orderService;

  @Scheduled(fixedDelayString = "PT1M")
  public void sweep() {
    orderService.autoRejectStaleOrders();
  }
}
