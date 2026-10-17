package com.mentorship.restaurant.restaurant.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mentorship.restaurant.restaurant.exception.MenuAlreadyExistsException;
import com.mentorship.restaurant.restaurant.exception.MenuCodeInUseException;
import com.mentorship.restaurant.restaurant.exception.MenuItemCodeInUseException;
import com.mentorship.restaurant.restaurant.model.entity.Menu;
import com.mentorship.restaurant.restaurant.model.entity.MenuItem;
import com.mentorship.restaurant.restaurant.model.entity.Restaurant;
import com.mentorship.restaurant.restaurant.model.mapper.MenuMapper;
import com.mentorship.restaurant.restaurant.model.request.CreateMenuItemRequest;
import com.mentorship.restaurant.restaurant.model.request.CreateMenuRequest;
import com.mentorship.restaurant.restaurant.repository.MenuItemRepository;
import com.mentorship.restaurant.restaurant.repository.MenuRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Two requests racing past the menu and code checks: the second trips a unique index. Over HTTP the
 * window cannot be hit on demand, so this is a unit test (CLAUDE.md, "Testing").
 */
@ExtendWith(MockitoExtension.class)
class MenuRaceTest {

  private static final long RESTAURANT = 4L;
  private static final long OWNER = 7L;

  @Mock private RestaurantService restaurantService;
  @Mock private MenuRepository menuRepository;
  @Mock private MenuItemRepository menuItemRepository;
  @Mock private MenuMapper menuMapper;
  @InjectMocks private MenuService menuService;

  @Test
  void aRacingSecondMenuIs409() {
    racingMenuCreate("uq_menus_active_restaurant");

    assertThatThrownBy(() -> menuService.createMenu(RESTAURANT, OWNER, menuRequest()))
        .isInstanceOf(MenuAlreadyExistsException.class)
        .hasMessage("Restaurant already has a menu");
  }

  @Test
  void aRacingMenuCodeIs409() {
    racingMenuCreate("menus_menu_code_key");

    assertThatThrownBy(() -> menuService.createMenu(RESTAURANT, OWNER, menuRequest()))
        .isInstanceOf(MenuCodeInUseException.class)
        .hasMessage("Menu code is already in use");
  }

  @Test
  void anyOtherViolationIsRethrown() {
    DataIntegrityViolationException tooLong = violationOf("some_other_constraint");
    when(restaurantService.findActiveRestaurant(RESTAURANT))
        .thenReturn(Restaurant.builder().build());
    when(menuRepository.save(any(Menu.class))).thenThrow(tooLong);

    assertThatThrownBy(() -> menuService.createMenu(RESTAURANT, OWNER, menuRequest()))
        .isSameAs(tooLong);
  }

  @Test
  void aRacingItemCodeIs409() {
    when(menuRepository.findActiveByRestaurantId(RESTAURANT))
        .thenReturn(Optional.of(Menu.builder().id(1L).build()));
    when(menuItemRepository.save(any(MenuItem.class)))
        .thenThrow(violationOf("menu_items_menu_item_code_key"));
    CreateMenuItemRequest request =
        new CreateMenuItemRequest("K-1", "Kofta", BigDecimal.TEN, 1, null, null, null);

    assertThatThrownBy(() -> menuService.createMenuItem(RESTAURANT, OWNER, request))
        .isInstanceOf(MenuItemCodeInUseException.class)
        .hasMessage("Item code is already in use");
  }

  private void racingMenuCreate(String constraint) {
    when(restaurantService.findActiveRestaurant(RESTAURANT))
        .thenReturn(Restaurant.builder().build());
    when(menuRepository.save(any(Menu.class))).thenThrow(violationOf(constraint));
  }

  private static CreateMenuRequest menuRequest() {
    return new CreateMenuRequest("KC-MAIN", "Main");
  }

  /** What Spring raises when Postgres refuses a row against {@code constraint}. */
  private static DataIntegrityViolationException violationOf(String constraint) {
    return new DataIntegrityViolationException(
        "duplicate key",
        new ConstraintViolationException("duplicate key", new SQLException(), constraint));
  }
}
