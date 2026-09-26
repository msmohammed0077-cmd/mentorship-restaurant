package com.mentorship.restaurant.order.service.handler;

import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.order.exception.OrderAlreadyRatedException;
import com.mentorship.restaurant.order.exception.OrderNotDeliveredException;
import com.mentorship.restaurant.order.exception.OrderNotFoundException;
import com.mentorship.restaurant.order.exception.OrderNotOwnedException;
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
public class RateOrderHandler {
  private final OrderRepository orderRepository;
  private final OrderRatingRepository orderRatingRepository;
  private final OrderRatingMapper orderRatingMapper;

  @Transactional
  public OrderRatingResponse rate(Long orderId, Long customerId, Integer score, String comment) {
    Order order =
        orderRepository
            .findByIdWithOwner(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));

    ensureCustomerOwnsOrder(order, customerId);
    ensureOwnerNotDeleted(order);
    ensureOrderIsDelivered(order);
    ensureOrderIsNotRated(orderId);

    OrderRating rating = new OrderRating();
    rating.setOrder(order);
    rating.setScore(score);
    rating.setComment(comment);
    rating.setCreatedAt(OffsetDateTime.now());

    try {
      return orderRatingMapper.toResponse(orderRatingRepository.saveAndFlush(rating));
    } catch (DataIntegrityViolationException exception) {
      throw new OrderAlreadyRatedException("Order is already rated");
    }
  }

  private void ensureCustomerOwnsOrder(Order order, Long customerId) {
    if (!order.getCustomer().getId().equals(customerId)) {
      throw new OrderNotOwnedException("Order belongs to another customer");
    }
  }

  /**
   * Runs after the ownership check, so the owner is the caller. A soft-deleted customer does not
   * exist to the API, even though their orders stay.
   */
  private void ensureOwnerNotDeleted(Order order) {
    if (order.getCustomer().getUser().getUserDeletedAt() != null) {
      throw new CustomerNotFoundException("Customer not found");
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
