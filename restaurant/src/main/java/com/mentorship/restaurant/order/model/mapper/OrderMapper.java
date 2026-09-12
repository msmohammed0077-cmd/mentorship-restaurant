package com.mentorship.restaurant.order.model.mapper;

import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.cart.model.entity.MenuItem;
import com.mentorship.restaurant.customer.model.entity.Address;
import com.mentorship.restaurant.order.model.entity.Order;
import com.mentorship.restaurant.order.model.entity.OrderItem;
import com.mentorship.restaurant.order.model.entity.OrderStatus;
import com.mentorship.restaurant.order.model.entity.Transaction;
import com.mentorship.restaurant.order.model.request.CreateOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderItemResponse;
import com.mentorship.restaurant.order.model.response.OrderResponse;
import com.mentorship.restaurant.order.model.response.TransactionResponse;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderMapper {

  private final OrderItemMapper orderItemMapper;

  public OrderResponse toResponse(Order order) {
    List<OrderItemResponse> items =
        order.getItems().stream().map(orderItemMapper::toItemResponse).toList();

    return OrderResponse.builder()
        .id(order.getId())
        .customerId(order.getCustomer().getId())
        .restaurantId(order.getRestaurant().getId())
        .addressId(order.getAddress().getId())
        .status(order.getStatus())
        .total(order.getTotal())
        .createdAt(order.getCreatedAt())
        .prepTimeMinutes(order.getPrepTimeMinutes())
        .rejectionReason(order.getRejectionReason())
        .rejectionNote(order.getRejectionNote())
        .orderItems(items)
        .customerNote(order.getCustomerNote())
        .transactions(toTransactionResponse(order.getTransaction()))
        .build();
  }

  public Order createNewOrderEntity(
      CreateOrderRequest request, Cart cart, Address address, Transaction transaction) {
    Order order = new Order();

    order.setStatus(OrderStatus.PLACED);
    order.setAddress(address);
    order.setTransaction(transaction);
    order.setCustomer(cart.getCustomer());
    order.setRestaurant(cart.getItems().get(0).getMenuItem().getMenu().getRestaurant());
    if (transaction != null) {
      transaction.setOrder(order);
    }

    order.setCustomerNote(request.getCustomerNote());

    List<OrderItem> orderItems =
        cart.getItems().stream().map(cartItem -> toOrderItem(cartItem, order)).toList();

    order.setItems(orderItems);
    order.setTotal(calculateTotal(orderItems));

    return order;
  }

  private BigDecimal calculateTotal(List<OrderItem> orderItems) {
    return orderItems.stream()
        .map(item -> item.getItemPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private OrderItem toOrderItem(CartItem cartItem, Order order) {

    MenuItem menuItem = cartItem.getMenuItem();

    OrderItem orderItem = new OrderItem();
    orderItem.setOrder(order);
    orderItem.setMenuItem(menuItem);
    orderItem.setItemName(menuItem.getName());
    orderItem.setQuantity(cartItem.getQuantity());
    orderItem.setItemPrice(cartItem.getItemPrice());

    return orderItem;
  }

  private TransactionResponse toTransactionResponse(Transaction transaction) {
    if (transaction == null) {
      return null;
    }
    return TransactionResponse.builder()
        .id(transaction.getId())
        .transactionProviderCode(transaction.getTransactionProviderCode())
        .transactionNumber(transaction.getTransactionNumber())
        .orderId(transaction.getOrder().getId())
        .status(transaction.getStatus())
        .transactionAmount(transaction.getTransactionAmount())
        .transactionDate(transaction.getTransactionDate())
        .build();
  }
}
