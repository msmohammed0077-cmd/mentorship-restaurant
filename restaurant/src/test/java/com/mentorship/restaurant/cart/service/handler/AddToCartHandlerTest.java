package com.mentorship.restaurant.cart.service.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mentorship.restaurant.cart.exception.CustomerNotFoundException;
import com.mentorship.restaurant.cart.exception.DifferentRestaurantException;
import com.mentorship.restaurant.cart.exception.InvalidQuantityException;
import com.mentorship.restaurant.cart.exception.MenuItemNotFoundException;
import com.mentorship.restaurant.cart.exception.OutOfStockException;
import com.mentorship.restaurant.cart.exception.RestaurantClosedException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.cart.model.entity.Customer;
import com.mentorship.restaurant.cart.model.entity.EntityFixtures;
import com.mentorship.restaurant.cart.model.entity.MenuItem;
import com.mentorship.restaurant.cart.model.entity.Restaurant;
import com.mentorship.restaurant.cart.model.mapper.CartItemMapper;
import com.mentorship.restaurant.cart.model.mapper.CartMapper;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import com.mentorship.restaurant.cart.repository.CartRepository;
import com.mentorship.restaurant.cart.repository.CustomerRepository;
import com.mentorship.restaurant.cart.repository.MenuItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddToCartHandlerTest {

  private static final Long CUSTOMER_ID = 3L;
  private static final Long MENU_ITEM_ID = 1L;
  private static final Long CART_ID = 9L;

  @Mock private CartRepository cartRepository;
  @Mock private CartItemRepository cartItemRepository;
  @Mock private CustomerRepository customerRepository;
  @Mock private MenuItemRepository menuItemRepository;

  private AddToCartHandler handler;

  private Customer customer;
  private MenuItem kofta;
  private Cart cart;

  @BeforeEach
  void setUp() {
    handler =
        new AddToCartHandler(
            cartRepository,
            cartItemRepository,
            customerRepository,
            menuItemRepository,
            new CartMapper(new CartItemMapper()));

    customer = EntityFixtures.customer(CUSTOMER_ID);
    kofta = menuItem(EntityFixtures.restaurant(1L, "Nile Kitchen", true), "185.00", 5);

    cart = new Cart();
    cart.setId(CART_ID);
    cart.setCustomer(customer);

    lenient().when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
    lenient().when(menuItemRepository.findById(MENU_ITEM_ID)).thenReturn(Optional.of(kofta));
    lenient()
        .when(cartItemRepository.findByCart_IdAndMenuItem_Id(CART_ID, MENU_ITEM_ID))
        .thenReturn(Optional.empty());
    lenient().when(cartRepository.findRestaurantIdsByCartId(CART_ID)).thenReturn(List.of());
    lenient().when(cartItemRepository.save(any(CartItem.class))).thenAnswer(c -> c.getArgument(0));
  }

  @Test
  void rejectsAQuantityOfZeroOrLess() {
    assertThatThrownBy(() -> handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 0, null))
        .isInstanceOf(InvalidQuantityException.class);
  }

  @Test
  void rejectsAnUnknownCustomer() {
    when(customerRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.addItem(99L, MENU_ITEM_ID, 1, null))
        .isInstanceOf(CustomerNotFoundException.class);
  }

  @Test
  void rejectsAnUnknownMenuItem() {
    when(menuItemRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.addItem(CUSTOMER_ID, 99L, 1, null))
        .isInstanceOf(MenuItemNotFoundException.class);
  }

  @Test
  void rejectsAClosedRestaurant() {
    MenuItem fries = menuItem(EntityFixtures.restaurant(2L, "Burger Yard", false), "35.00", 5);
    when(menuItemRepository.findById(MENU_ITEM_ID)).thenReturn(Optional.of(fries));

    assertThatThrownBy(() -> handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 1, null))
        .isInstanceOf(RestaurantClosedException.class)
        .hasMessage("Restaurant is closed");
  }

  @Test
  void rejectsAQuantityBeyondStock() {
    when(cartRepository.findByCustomer_Id(CUSTOMER_ID)).thenReturn(Optional.of(cart));

    assertThatThrownBy(() -> handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 6, null))
        .isInstanceOf(OutOfStockException.class)
        .hasMessage("Item is not in stock");
  }

  @Test
  void rejectsAnIncrementThatWouldExceedStock() {
    CartItem existing = line(kofta, 3);
    when(cartRepository.findByCustomer_Id(CUSTOMER_ID)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findByCart_IdAndMenuItem_Id(CART_ID, MENU_ITEM_ID))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 3, null))
        .isInstanceOf(OutOfStockException.class)
        .hasMessage("Item is not in stock");
  }

  @Test
  void rejectsAnItemFromADifferentRestaurant() {
    when(cartRepository.findByCustomer_Id(CUSTOMER_ID)).thenReturn(Optional.of(cart));
    when(cartRepository.findRestaurantIdsByCartId(CART_ID)).thenReturn(List.of(2L));

    assertThatThrownBy(() -> handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 1, null))
        .isInstanceOf(DifferentRestaurantException.class)
        .hasMessage("Item is of different restaurant");
  }

  @Test
  void acceptsAnyRestaurantIntoAnEmptyCart() {
    when(cartRepository.findByCustomer_Id(CUSTOMER_ID)).thenReturn(Optional.of(cart));
    when(cartRepository.findRestaurantIdsByCartId(CART_ID)).thenReturn(List.of());

    handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 1, null);

    verify(cartItemRepository).save(any(CartItem.class));
  }

  @Test
  void createsACartWhenTheCustomerHasNone() {
    when(cartRepository.findByCustomer_Id(CUSTOMER_ID)).thenReturn(Optional.empty());
    when(cartRepository.save(any(Cart.class)))
        .thenAnswer(
            call -> {
              Cart saved = call.getArgument(0);
              saved.setId(CART_ID);
              return saved;
            });

    handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 2, null);

    verify(cartRepository).save(any(Cart.class));
  }

  @Test
  void reusesTheExistingCart() {
    when(cartRepository.findByCustomer_Id(CUSTOMER_ID)).thenReturn(Optional.of(cart));

    handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 2, null);

    verify(cartRepository, never()).save(any(Cart.class));
  }

  @Test
  void capturesTheMenuPriceOnANewLine() {
    when(cartRepository.findByCustomer_Id(CUSTOMER_ID)).thenReturn(Optional.of(cart));

    handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 2, "extra spicy");

    assertThat(cart.getItems()).hasSize(1);
    assertThat(cart.getItems().get(0).getItemPrice()).isEqualByComparingTo("185.00");
    assertThat(cart.getItems().get(0).getNote()).isEqualTo("extra spicy");
  }

  @Test
  void incrementsTheExistingLineAndKeepsItsPrice() {
    CartItem existing = line(kofta, 1);
    existing.setItemPrice(new BigDecimal("150.00"));
    when(cartRepository.findByCustomer_Id(CUSTOMER_ID)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findByCart_IdAndMenuItem_Id(CART_ID, MENU_ITEM_ID))
        .thenReturn(Optional.of(existing));

    handler.addItem(CUSTOMER_ID, MENU_ITEM_ID, 2, null);

    assertThat(existing.getQuantity()).isEqualTo(3);
    assertThat(existing.getItemPrice()).isEqualByComparingTo("150.00");
    verify(cartItemRepository, never()).save(any(CartItem.class));
  }

  private MenuItem menuItem(Restaurant restaurant, String price, int stock) {
    return EntityFixtures.menuItem(
        MENU_ITEM_ID, EntityFixtures.menu(restaurant), "Kofta Platter", price, stock);
  }

  private CartItem line(MenuItem menuItem, int quantity) {
    CartItem item = new CartItem();
    item.setCart(cart);
    item.setMenuItem(menuItem);
    item.setQuantity(quantity);
    item.setItemPrice(menuItem.getItemPrice());
    cart.getItems().add(item);
    return item;
  }
}
