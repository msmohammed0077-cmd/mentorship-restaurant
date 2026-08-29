package com.mentorship.restaurant.cart.service.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.exception.CartItemNotFoundException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.entity.Customer;
import com.mentorship.restaurant.cart.model.mapper.CartItemMapper;
import com.mentorship.restaurant.cart.model.mapper.CartMapper;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import com.mentorship.restaurant.cart.repository.CartRepository;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ViewCartHandlerTest {

  private final CartItemRepository cartItemRepository = mock(CartItemRepository.class);
  private final CartRepository cartRepository = mock(CartRepository.class);
  private final CartMapper cartMapper = new CartMapper(mock(CartItemMapper.class));
  private final ViewCartHandler handler =
      new ViewCartHandler(cartItemRepository, cartRepository, cartMapper);

  @Test
  void returnsCustomerIdForEmptyCart() {
    Cart cart = mock(Cart.class);
    Customer customer = mock(Customer.class);
    when(cartRepository.findById(7L)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findAllByCart_Id(7L)).thenReturn(Optional.of(Collections.emptyList()));
    when(cart.getId()).thenReturn(7L);
    when(cart.getCustomer()).thenReturn(customer);
    when(cart.getItems()).thenReturn(Collections.emptyList());
    when(customer.getId()).thenReturn(42L);

    CartResponse response = handler.viewCart(7L);

    assertThat(response.getCustomerId()).isEqualTo(42L);
  }

  @Test
  void throwsWhenCartDoesNotExist() {
    when(cartRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.viewCart(99L)).isInstanceOf(CartItemNotFoundException.class);
    verify(cartItemRepository, never()).findAllByCart_Id(99L);
  }
}
