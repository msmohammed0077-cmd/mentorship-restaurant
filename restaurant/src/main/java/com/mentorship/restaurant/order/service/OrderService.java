package com.mentorship.restaurant.order.service;

import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.service.CartService;
import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.model.entity.Address;
import com.mentorship.restaurant.customer.service.AddressService;
import com.mentorship.restaurant.order.exception.OrderNotFoundException;
import com.mentorship.restaurant.order.model.entity.Order;
import com.mentorship.restaurant.order.model.entity.OrderStatus;
import com.mentorship.restaurant.order.model.mapper.OrderMapper;
import com.mentorship.restaurant.order.model.request.CreateOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderResponse;
import com.mentorship.restaurant.order.repository.OrderRepository;
import com.mentorship.restaurant.order.service.createorder.AddressValidatorHandler;
import com.mentorship.restaurant.order.service.createorder.CartValidatorHandler;
import com.mentorship.restaurant.order.service.createorder.ItemsValidatorHandler;
import com.mentorship.restaurant.order.service.createorder.OrderFinalizer;
import com.mentorship.restaurant.order.service.createorder.OrderHandler;
import com.mentorship.restaurant.order.service.createorder.ProcessPaymentHandler;
import com.mentorship.restaurant.order.service.createorder.SendNotificationHandler;
import com.mentorship.restaurant.payment.service.PaymentProcessor;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creating and viewing orders, plus the guard the other order services share. Those services inject
 * this one; this one never injects them, so Spring cannot meet a cycle.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderMapper orderMapper;
  private final CartService cartService;
  private final AddressService addressService;
  private final PaymentProcessor paymentProcessor;

  /** A chain of responsibility: each link validates or acts, then hands on to the next. */
  @Transactional
  public OrderResponse createOrder(Long customerId, CreateOrderRequest request) {
    Cart cart = cartService.findCart(request.getCartId());
    Address address = addressService.findAddress(request.getAddressId());

    OrderHandler orderHandler =
        OrderHandler.processOrder(
            new CartValidatorHandler(cart, customerId),
            new AddressValidatorHandler(address, customerId),
            new ItemsValidatorHandler(cart),
            new ProcessPaymentHandler(paymentProcessor),
            new OrderFinalizer(cart, address, orderMapper, orderRepository, cartService),
            new SendNotificationHandler());

    OrderResponse response =
        OrderResponse.builder()
            .customerId(customerId)
            .restaurantId(request.getRestaurantId())
            .build();

    return orderHandler.handle(request, response);
  }

  /** The caller's order: placed by their customer, or with their restaurant; else a 404. */
  @Transactional(readOnly = true)
  public OrderResponse viewOrderDetails(Long orderId, Long customerId, Long userId) {
    Order order =
        orderRepository
            .findByIdForCaller(orderId, customerId, userId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));

    return orderMapper.toResponse(order);
  }

  /**
   * Whether the customer has an order in a non-terminal status. For delete-customer; joins the
   * caller's transaction.
   */
  public boolean hasActiveOrders(Long customerId) {
    return orderRepository.existsByCustomer_IdAndStatusIn(customerId, OrderStatus.ACTIVE);
  }

  /**
   * Shared: a soft-deleted customer does not exist to the API, even though their orders stay for
   * history, so acting on them is a 404. Callers run it after the ownership check, so the owner is
   * the caller.
   */
  public void ensureOwnerActive(OffsetDateTime ownerDeletedAt) {
    if (ownerDeletedAt != null) {
      throw new CustomerNotFoundException("Customer not found");
    }
  }
}
