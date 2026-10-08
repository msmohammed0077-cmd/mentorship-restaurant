package com.mentorship.restaurant.order.model.mapper;

import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.customer.model.entity.Address;
import com.mentorship.restaurant.order.model.entity.Order;
import com.mentorship.restaurant.order.model.entity.OrderItem;
import com.mentorship.restaurant.order.model.entity.OrderStatus;
import com.mentorship.restaurant.order.model.request.CreateOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderItemResponse;
import com.mentorship.restaurant.order.model.response.OrderResponse;
import com.mentorship.restaurant.order.model.response.TransactionResponse;
import com.mentorship.restaurant.payment.model.entity.Transaction;
import com.mentorship.restaurant.restaurant.model.entity.MenuItem;
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
        .transactionResponse(toTransactionResponse(order.getTransaction()))
        .build();
  }

  public Order createNewOrderEntity(
      CreateOrderRequest request, Cart cart, Address address, Transaction transaction) {
    Order order =
        Order.builder()
            .status(OrderStatus.PLACED)
            .address(address)
            .transaction(transaction)
            .customer(cart.getCustomer())
            .restaurant(cart.getItems().get(0).getMenuItem().getMenu().getRestaurant())
            .customerNote(request.getCustomerNote())
            .build();
    if (transaction != null) {
      transaction.setOrder(order);
    }

    // The items point back at the order, so they are built after it.
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

    return OrderItem.builder()
        .order(order)
        .menuItem(menuItem)
        .itemName(menuItem.getName())
        .quantity(cartItem.getQuantity())
        .itemPrice(cartItem.getItemPrice())
        .build();
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
