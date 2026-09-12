package com.mentorship.restaurant.order.service.handler;

import com.mentorship.restaurant.cart.exception.CartNotFoundException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.repository.CartRepository;
import com.mentorship.restaurant.customer.exception.AddressNotFoundException;
import com.mentorship.restaurant.customer.model.entity.Address;
import com.mentorship.restaurant.customer.repository.AddressRepository;
import com.mentorship.restaurant.order.model.entity.Order;
import com.mentorship.restaurant.order.model.entity.Transaction;
import com.mentorship.restaurant.order.model.mapper.OrderMapper;
import com.mentorship.restaurant.order.model.request.CreateOrderRequest;
import com.mentorship.restaurant.order.model.request.PaymentMethod;
import com.mentorship.restaurant.order.model.response.OrderResponse;
import com.mentorship.restaurant.order.repository.OrderRepository;
import com.mentorship.restaurant.support.PaymentProcesser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateOrderHandler {

  private final OrderMapper orderMapper;
  private final OrderRepository orderRepository;
  private final CartRepository cartRepository;
  private final AddressRepository addressRepository;
  private final PaymentProcesser paymentProcesser;

  @Transactional
  public OrderResponse createOrder(CreateOrderRequest request) {

    Cart cart =
        cartRepository
            .findById(request.getCartId())
            .orElseThrow(() -> new CartNotFoundException("Cart Not Found"));
    Address address =
        addressRepository
            .findByIdAndCustomer_Id(request.getAddressId(), cart.getCustomer().getId())
            .orElseThrow(() -> new AddressNotFoundException("Address Not Found"));

    Transaction transaction = null;
    if (PaymentMethod.CARD.equals(request.getPaymentMethod())) {
      transaction = paymentProcesser.process(request.getCardId());
    }

    Order order = orderMapper.createNewOrderEntity(request, cart, address, transaction);

    Order savedOrder = orderRepository.save(order);

    // Todo: notify restaurant
    // Todo: find and notify driver

    return orderMapper.toResponse(savedOrder);
  }
}
