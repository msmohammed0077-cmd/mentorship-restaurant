package com.mentorship.restaurant.restaurant.service;

import com.mentorship.restaurant.restaurant.exception.MenuAlreadyExistsException;
import com.mentorship.restaurant.restaurant.exception.MenuCodeInUseException;
import com.mentorship.restaurant.restaurant.exception.MenuItemCodeInUseException;
import com.mentorship.restaurant.restaurant.exception.MenuItemNotFoundException;
import com.mentorship.restaurant.restaurant.exception.MenuNotFoundException;
import com.mentorship.restaurant.restaurant.model.entity.Menu;
import com.mentorship.restaurant.restaurant.model.entity.MenuItem;
import com.mentorship.restaurant.restaurant.model.entity.Restaurant;
import com.mentorship.restaurant.restaurant.model.mapper.MenuMapper;
import com.mentorship.restaurant.restaurant.model.request.CreateMenuItemRequest;
import com.mentorship.restaurant.restaurant.model.request.CreateMenuRequest;
import com.mentorship.restaurant.restaurant.model.request.SetMenuItemAvailableRequest;
import com.mentorship.restaurant.restaurant.model.request.UpdateMenuItemRequest;
import com.mentorship.restaurant.restaurant.model.request.UpdateMenuRequest;
import com.mentorship.restaurant.restaurant.model.response.MenuItemResponse;
import com.mentorship.restaurant.restaurant.model.response.MenuResponse;
import com.mentorship.restaurant.restaurant.repository.MenuItemRepository;
import com.mentorship.restaurant.restaurant.repository.MenuRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A restaurant's one menu and its items (#92), behind {@link
 * com.mentorship.restaurant.restaurant.controller.MenuController}. Every write is the owner's:
 * {@link RestaurantService#ensureOwnedBy} runs first, before anything is read. Deletes are soft, so
 * past orders and carts keep their lines.
 */
@Service
@RequiredArgsConstructor
public class MenuService {

  private static final String ACTIVE_MENU_INDEX = "uq_menus_active_restaurant";
  private static final String MENU_CODE_CONSTRAINT = "menus_menu_code_key";
  private static final String ITEM_CODE_CONSTRAINT = "menu_items_menu_item_code_key";

  private final RestaurantService restaurantService;
  private final MenuRepository menuRepository;
  private final MenuItemRepository menuItemRepository;
  private final MenuMapper menuMapper;

  /** Public. Every item that is not deleted, available or not. */
  @Transactional(readOnly = true)
  public MenuResponse getMenu(Long restaurantId) {
    restaurantService.findActiveRestaurant(restaurantId);
    Menu menu = findActiveMenu(restaurantId);
    return toResponse(menu);
  }

  @Transactional
  public MenuResponse createMenu(Long restaurantId, Long callerUserId, CreateMenuRequest request) {
    restaurantService.ensureOwnedBy(restaurantId, callerUserId);
    Restaurant restaurant = restaurantService.findActiveRestaurant(restaurantId);
    ensureNoActiveMenu(restaurantId);
    ensureMenuCodeAvailable(request.getCode());

    Menu menu =
        Menu.builder()
            .restaurant(restaurant)
            .menuCode(request.getCode())
            .menuCategory(request.getCategory())
            .build();
    // The IDENTITY insert runs at once, so a racing request's violation surfaces here.
    saveOrConflict(() -> menuRepository.save(menu));
    return menuMapper.toResponse(menu, List.of());
  }

  /** Absent or null fields are left as they are. */
  @Transactional
  public MenuResponse updateMenu(Long restaurantId, Long callerUserId, UpdateMenuRequest request) {
    restaurantService.ensureOwnedBy(restaurantId, callerUserId);
    Menu menu = findActiveMenu(restaurantId);

    if (request.getCode() != null) {
      ensureMenuCodeAvailableExcept(request.getCode(), menu.getId());
      menu.setMenuCode(request.getCode());
    }
    if (request.getCategory() != null) {
      menu.setMenuCategory(request.getCategory());
    }
    // Dirty checking would write at commit, after this method: flush to answer a race with 409.
    saveOrConflict(menuRepository::flush);
    return toResponse(menu);
  }

  /** The menu and its items, each in one statement. The restaurant may then create a new menu. */
  @Transactional
  public void deleteMenu(Long restaurantId, Long callerUserId) {
    restaurantService.ensureOwnedBy(restaurantId, callerUserId);
    Long menuId = findActiveMenu(restaurantId).getId();
    OffsetDateTime now = OffsetDateTime.now();

    menuItemRepository.softDeleteAllOfMenu(menuId, now);
    menuRepository.softDelete(menuId, now);
  }

  @Transactional
  public MenuItemResponse createMenuItem(
      Long restaurantId, Long callerUserId, CreateMenuItemRequest request) {
    restaurantService.ensureOwnedBy(restaurantId, callerUserId);
    Menu menu = findActiveMenu(restaurantId);
    ensureItemCodeAvailable(request.getCode());

    MenuItem item =
        MenuItem.builder()
            .menu(menu)
            .menuItemCode(request.getCode())
            .name(request.getName())
            .itemPrice(request.getPrice())
            .stock(request.getStock())
            .note(request.getNote())
            .menuItemImageUrl(request.getImageUrl())
            .isAvailable(!Boolean.FALSE.equals(request.getIsAvailable()))
            .build();
    saveOrConflict(() -> menuItemRepository.save(item));
    return menuMapper.toItemResponse(item);
  }

  /** Absent or null fields are left as they are. A cart line keeps the price it was added at. */
  @Transactional
  public MenuItemResponse updateMenuItem(
      Long restaurantId, Long menuItemId, Long callerUserId, UpdateMenuItemRequest request) {
    restaurantService.ensureOwnedBy(restaurantId, callerUserId);
    MenuItem item = findItemOnMenu(findActiveMenu(restaurantId), menuItemId);

    if (request.getCode() != null) {
      ensureItemCodeAvailableExcept(request.getCode(), item.getId());
      item.setMenuItemCode(request.getCode());
    }
    if (request.getName() != null) {
      item.setName(request.getName());
    }
    if (request.getPrice() != null) {
      item.setItemPrice(request.getPrice());
    }
    if (request.getStock() != null) {
      item.setStock(request.getStock());
    }
    if (request.getNote() != null) {
      item.setNote(request.getNote());
    }
    if (request.getImageUrl() != null) {
      item.setMenuItemImageUrl(request.getImageUrl());
    }
    saveOrConflict(menuItemRepository::flush);
    return menuMapper.toItemResponse(item);
  }

  /** Idempotent. An unavailable item stays on the menu but cannot be carted or ordered. */
  @Transactional
  public MenuItemResponse setMenuItemAvailable(
      Long restaurantId, Long menuItemId, Long callerUserId, SetMenuItemAvailableRequest request) {
    restaurantService.ensureOwnedBy(restaurantId, callerUserId);
    MenuItem item = findItemOnMenu(findActiveMenu(restaurantId), menuItemId);

    // Lombok strips "is" from the setter of boolean isAvailable: setAvailable.
    item.setAvailable(request.getIsAvailable());
    return menuMapper.toItemResponse(item);
  }

  @Transactional
  public void deleteMenuItem(Long restaurantId, Long menuItemId, Long callerUserId) {
    restaurantService.ensureOwnedBy(restaurantId, callerUserId);
    MenuItem item = findItemOnMenu(findActiveMenu(restaurantId), menuItemId);

    item.setMenuItemDeletedAt(OffsetDateTime.now());
  }

  private MenuResponse toResponse(Menu menu) {
    return menuMapper.toResponse(
        menu, menuItemRepository.findByMenu_IdAndMenuItemDeletedAtIsNullOrderById(menu.getId()));
  }

  /** Also not found when the restaurant is deleted. */
  private Menu findActiveMenu(Long restaurantId) {
    return menuRepository
        .findActiveByRestaurantId(restaurantId)
        .orElseThrow(() -> new MenuNotFoundException("Menu not found"));
  }

  private MenuItem findItemOnMenu(Menu menu, Long menuItemId) {
    return menuItemRepository
        .findByIdAndMenu_IdAndMenuItemDeletedAtIsNull(menuItemId, menu.getId())
        .orElseThrow(() -> new MenuItemNotFoundException("Item not found"));
  }

  private void ensureNoActiveMenu(Long restaurantId) {
    if (menuRepository.existsByRestaurant_IdAndMenuDeletedAtIsNull(restaurantId)) {
      throw new MenuAlreadyExistsException("Restaurant already has a menu");
    }
  }

  private void ensureMenuCodeAvailable(String code) {
    if (menuRepository.existsByMenuCode(code)) {
      throw new MenuCodeInUseException("Menu code is already in use");
    }
  }

  private void ensureMenuCodeAvailableExcept(String code, Long menuId) {
    if (menuRepository.existsByMenuCodeAndIdNot(code, menuId)) {
      throw new MenuCodeInUseException("Menu code is already in use");
    }
  }

  private void ensureItemCodeAvailable(String code) {
    if (menuItemRepository.existsByMenuItemCode(code)) {
      throw new MenuItemCodeInUseException("Item code is already in use");
    }
  }

  private void ensureItemCodeAvailableExcept(String code, Long menuItemId) {
    if (menuItemRepository.existsByMenuItemCodeAndIdNot(code, menuItemId)) {
      throw new MenuItemCodeInUseException("Item code is already in use");
    }
  }

  /**
   * The unique indexes are the backstop for two requests racing past the checks above. Any other
   * violation is a bug, and is rethrown to stay a 500 rather than be reported as a conflict.
   */
  private static void saveOrConflict(Runnable write) {
    try {
      write.run();
    } catch (DataIntegrityViolationException exception) {
      throw conflictOrRethrow(exception);
    }
  }

  private static RuntimeException conflictOrRethrow(DataIntegrityViolationException exception) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation) {
        String constraint = violation.getConstraintName();
        if (ACTIVE_MENU_INDEX.equals(constraint)) {
          return new MenuAlreadyExistsException("Restaurant already has a menu");
        }
        if (MENU_CODE_CONSTRAINT.equals(constraint)) {
          return new MenuCodeInUseException("Menu code is already in use");
        }
        if (ITEM_CODE_CONSTRAINT.equals(constraint)) {
          return new MenuItemCodeInUseException("Item code is already in use");
        }
      }
    }
    return exception;
  }
}
