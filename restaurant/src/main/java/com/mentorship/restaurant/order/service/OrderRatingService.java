package com.mentorship.restaurant.order.service;

import com.mentorship.restaurant.order.exception.OrderAlreadyRatedException;
import com.mentorship.restaurant.order.exception.OrderNotDeliveredException;
import com.mentorship.restaurant.order.exception.OrderNotFoundException;
import com.mentorship.restaurant.order.model.entity.Order;
import com.mentorship.restaurant.order.model.entity.OrderRating;
import com.mentorship.restaurant.order.model.entity.OrderStatus;
import com.mentorship.restaurant.order.model.mapper.OrderRatingMapper;
import com.mentorship.restaurant.order.model.response.OrderRatingResponse;
import com.mentorship.restaurant.order.repository.OrderRatingRepository;
import com.mentorship.restaurant.order.repository.OrderRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderRatingService {

  private final OrderRepository orderRepository;
  private final OrderRatingRepository orderRatingRepository;
  private final OrderRatingMapper orderRatingMapper;
  private final OrderService orderService;

  @Transactional
  public OrderRatingResponse rateOrder(Long orderId, Long customerId, Integer score, String comment) {
    Order order =
        orderRepository
            .findByIdWithOwner(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));

    orderService.ensureOwnedBy(
        order.getCustomer().getId(), customerId, "Order belongs to another customer");
    // Runs after the ownership check, so the owner is the caller.
    orderService.ensureOwnerActive(order.getCustomer().getUser().getUserDeletedAt());
    ensureOrderIsDelivered(order);
    ensureOrderIsNotRated(orderId);

    OrderRating rating =
        OrderRating.builder()
            .order(order)
            .score(score)
            .comment(comment)
            .createdAt(OffsetDateTime.now())
            .build();

    try {
      return orderRatingMapper.toResponse(orderRatingRepository.saveAndFlush(rating));
    } catch (DataIntegrityViolationException exception) {
      throw new OrderAlreadyRatedException("Order is already rated");
    }
  }

  private void ensureOrderIsDelivered(Order order) {
    if (order.getStatus() != OrderStatus.DELIVERED) {
      throw new OrderNotDeliveredException("Order is not delivered");
    }
  }

  private void ensureOrderIsNotRated(Long orderId) {
    if (orderRatingRepository.existsByOrderId(orderId)) {
      throw new OrderAlreadyRatedException("Order is already rated");
    }
  }
}
