package com.mentorship.restaurant.order.service.handler;

import com.mentorship.restaurant.cart.repository.MenuItemRepository;
import com.mentorship.restaurant.order.repository.OrderItemRepository;
import com.mentorship.restaurant.order.repository.OrderLineProjection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompensateOrderHandler {
  private final OrderItemRepository orderItemRepository;
  private final MenuItemRepository menuItemRepository;

  @Transactional(propagation = Propagation.MANDATORY)
  public void compensate(Long orderId) {
    List<OrderLineProjection> lines = orderItemRepository.findLinesByOrderId(orderId);
    lines.forEach(
        line -> menuItemRepository.incrementStock(line.getMenuItemId(), line.getQuantity()));
  }
}
