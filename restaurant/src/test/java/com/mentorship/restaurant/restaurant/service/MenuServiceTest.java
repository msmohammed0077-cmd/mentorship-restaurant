
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

import org.hibernate.exception.ConstraintViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    private static final Long RESTAURANT_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final Long MENU_ID = 10L;
    private static final Long MENU_ITEM_ID = 20L;

    @Mock
    private RestaurantService restaurantService;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private MenuMapper menuMapper;

    @InjectMocks
    private MenuService menuService;

    private Restaurant restaurant;
    private Menu menu;
    private MenuItem menuItem;
    private MenuResponse menuResponse;
    private MenuItemResponse menuItemResponse;

    @BeforeEach
    void setUp() {
        restaurant = mock(Restaurant.class);
        menu = mock(Menu.class);
        menuItem = mock(MenuItem.class);
        menuResponse = mock(MenuResponse.class);
        menuItemResponse = mock(MenuItemResponse.class);
    }

    // ---------------------------------------------------------
    // GET MENU
    // ---------------------------------------------------------

    @Test
    void getMenu_shouldReturnMenuWithItems() {
        List<MenuItem> items = List.of(menuItem);

        when(restaurantService.findActiveRestaurant(RESTAURANT_ID))
                .thenReturn(restaurant);
        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository
                .findByMenu_IdAndMenuItemDeletedAtIsNullOrderById(MENU_ID))
                .thenReturn(items);
        when(menuMapper.toResponse(menu, items))
                .thenReturn(menuResponse);

        MenuResponse result = menuService.getMenu(RESTAURANT_ID);

        assertSame(menuResponse, result);

        verify(restaurantService).findActiveRestaurant(RESTAURANT_ID);
        verify(menuRepository).findActiveByRestaurantId(RESTAURANT_ID);
        verify(menuItemRepository)
                .findByMenu_IdAndMenuItemDeletedAtIsNullOrderById(MENU_ID);
        verify(menuMapper).toResponse(menu, items);
    }

    @Test
    void getMenu_shouldThrowWhenMenuDoesNotExist() {
        when(restaurantService.findActiveRestaurant(RESTAURANT_ID))
                .thenReturn(restaurant);
        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                MenuNotFoundException.class,
                () -> menuService.getMenu(RESTAURANT_ID)
        );

        verify(menuItemRepository, never())
                .findByMenu_IdAndMenuItemDeletedAtIsNullOrderById(anyLong());
        verifyNoInteractions(menuMapper);
    }

    // ---------------------------------------------------------
    // CREATE MENU
    // ---------------------------------------------------------

    @Test
    void createMenu_shouldCreateMenuSuccessfully() {
        CreateMenuRequest request = createMenuRequest();

        when(restaurantService.findActiveRestaurant(RESTAURANT_ID))
                .thenReturn(restaurant);
        when(menuRepository
                .existsByRestaurant_IdAndMenuDeletedAtIsNull(RESTAURANT_ID))
                .thenReturn(false);
        when(menuRepository.existsByMenuCode("MAIN_MENU"))
                .thenReturn(false);
        when(menuMapper.toResponse(any(Menu.class), eq(List.of())))
                .thenReturn(menuResponse);

        MenuResponse result =
                menuService.createMenu(RESTAURANT_ID, USER_ID, request);

        assertSame(menuResponse, result);

        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        verify(menuRepository).save(captor.capture());

        Menu savedMenu = captor.getValue();
        assertEquals("MAIN_MENU", savedMenu.getMenuCode());
        assertEquals("MAIN", savedMenu.getMenuCategory());
        assertSame(restaurant, savedMenu.getRestaurant());

        verify(restaurantService).ensureOwnedBy(RESTAURANT_ID, USER_ID);
        verify(restaurantService).findActiveRestaurant(RESTAURANT_ID);
    }

    @Test
    void createMenu_shouldThrowWhenRestaurantAlreadyHasActiveMenu() {
        CreateMenuRequest request = createMenuRequest();

        when(menuRepository
                .existsByRestaurant_IdAndMenuDeletedAtIsNull(RESTAURANT_ID))
                .thenReturn(true);

        assertThrows(
                MenuAlreadyExistsException.class,
                () -> menuService.createMenu(RESTAURANT_ID, USER_ID, request)
        );

        verify(menuRepository, never()).save(any(Menu.class));
        verify(menuRepository, never()).existsByMenuCode(anyString());
    }

    @Test
    void createMenu_shouldThrowWhenMenuCodeAlreadyExists() {
        CreateMenuRequest request = createMenuRequest();

        when(menuRepository
                .existsByRestaurant_IdAndMenuDeletedAtIsNull(RESTAURANT_ID))
                .thenReturn(false);
        when(menuRepository.existsByMenuCode("MAIN_MENU"))
                .thenReturn(true);

        assertThrows(
                MenuCodeInUseException.class,
                () -> menuService.createMenu(RESTAURANT_ID, USER_ID, request)
        );

        verify(menuRepository, never()).save(any(Menu.class));
    }

    @Test
    void createMenu_shouldTranslateActiveMenuConstraintViolation() {
        CreateMenuRequest request = createMenuRequest();

        when(menuRepository
                .existsByRestaurant_IdAndMenuDeletedAtIsNull(RESTAURANT_ID))
                .thenReturn(false);
        when(menuRepository.existsByMenuCode("MAIN_MENU"))
                .thenReturn(false);
        doThrow(constraintViolation("uq_menus_active_restaurant"))
                .when(menuRepository).save(any(Menu.class));

        assertThrows(
                MenuAlreadyExistsException.class,
                () -> menuService.createMenu(RESTAURANT_ID, USER_ID, request)
        );
    }

    @Test
    void createMenu_shouldTranslateMenuCodeConstraintViolation() {
        CreateMenuRequest request = createMenuRequest();

        when(menuRepository
                .existsByRestaurant_IdAndMenuDeletedAtIsNull(RESTAURANT_ID))
                .thenReturn(false);
        when(menuRepository.existsByMenuCode("MAIN_MENU"))
                .thenReturn(false);
        doThrow(constraintViolation("menus_menu_code_key"))
                .when(menuRepository).save(any(Menu.class));

        assertThrows(
                MenuCodeInUseException.class,
                () -> menuService.createMenu(RESTAURANT_ID, USER_ID, request)
        );
    }

    // ---------------------------------------------------------
    // UPDATE MENU
    // ---------------------------------------------------------

    @Test
    void updateMenu_shouldUpdateProvidedFields() {
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setCode("UPDATED_MENU");
        request.setCategory("DESSERT");

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);
        when(menuRepository.existsByMenuCodeAndIdNot("UPDATED_MENU", MENU_ID))
                .thenReturn(false);
        when(menuItemRepository
                .findByMenu_IdAndMenuItemDeletedAtIsNullOrderById(MENU_ID))
                .thenReturn(List.of());
        when(menuMapper.toResponse(menu, List.of()))
                .thenReturn(menuResponse);

        MenuResponse result =
                menuService.updateMenu(RESTAURANT_ID, USER_ID, request);

        assertSame(menuResponse, result);
        verify(menu).setMenuCode("UPDATED_MENU");
        verify(menu).setMenuCategory("DESSERT");
        verify(menuRepository).flush();
    }

    @Test
    void updateMenu_shouldLeaveNullFieldsUnchanged() {
        UpdateMenuRequest request = new UpdateMenuRequest();

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository
                .findByMenu_IdAndMenuItemDeletedAtIsNullOrderById(MENU_ID))
                .thenReturn(List.of());
        when(menuMapper.toResponse(menu, List.of()))
                .thenReturn(menuResponse);

        MenuResponse result =
                menuService.updateMenu(RESTAURANT_ID, USER_ID, request);

        assertSame(menuResponse, result);
        verify(menu, never()).setMenuCode(anyString());
        verify(menu, never()).setMenuCategory(anyString());
        verify(menuRepository).flush();
    }

    @Test
    void updateMenu_shouldAllowUpdatingOnlyCategory() {
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setCategory("DESSERT");

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository
                .findByMenu_IdAndMenuItemDeletedAtIsNullOrderById(MENU_ID))
                .thenReturn(List.of());
        when(menuMapper.toResponse(menu, List.of()))
                .thenReturn(menuResponse);

        menuService.updateMenu(RESTAURANT_ID, USER_ID, request);

        verify(menu, never()).setMenuCode(anyString());
        verify(menu).setMenuCategory("DESSERT");
    }

    @Test
    void updateMenu_shouldAllowUpdatingOnlyCode() {
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setCode("UPDATED_MENU");

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);
        when(menuRepository.existsByMenuCodeAndIdNot("UPDATED_MENU", MENU_ID))
                .thenReturn(false);
        when(menuItemRepository
                .findByMenu_IdAndMenuItemDeletedAtIsNullOrderById(MENU_ID))
                .thenReturn(List.of());
        when(menuMapper.toResponse(menu, List.of()))
                .thenReturn(menuResponse);

        menuService.updateMenu(RESTAURANT_ID, USER_ID, request);

        verify(menu).setMenuCode("UPDATED_MENU");
        verify(menu, never()).setMenuCategory(anyString());
    }

    @Test
    void updateMenu_shouldThrowWhenNewCodeAlreadyExists() {
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setCode("EXISTING_MENU");

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);
        when(menuRepository.existsByMenuCodeAndIdNot("EXISTING_MENU", MENU_ID))
                .thenReturn(true);

        assertThrows(
                MenuCodeInUseException.class,
                () -> menuService.updateMenu(RESTAURANT_ID, USER_ID, request)
        );

        verify(menu, never()).setMenuCode(anyString());
        verify(menuRepository, never()).flush();
    }

    @Test
    void updateMenu_shouldThrowWhenMenuDoesNotExist() {
        UpdateMenuRequest request = new UpdateMenuRequest();

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                MenuNotFoundException.class,
                () -> menuService.updateMenu(RESTAURANT_ID, USER_ID, request)
        );

        verify(menuRepository, never()).flush();
    }

    @Test
    void updateMenu_shouldTranslateMenuCodeConstraintViolation() {
        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setCode("UPDATED_MENU");

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);
        when(menuRepository.existsByMenuCodeAndIdNot("UPDATED_MENU", MENU_ID))
                .thenReturn(false);
        doThrow(constraintViolation("menus_menu_code_key"))
                .when(menuRepository).flush();

        assertThrows(
                MenuCodeInUseException.class,
                () -> menuService.updateMenu(RESTAURANT_ID, USER_ID, request)
        );
    }

    // ---------------------------------------------------------
    // DELETE MENU
    // ---------------------------------------------------------

    @Test
    void deleteMenu_shouldSoftDeleteMenuAndItsItems() {
        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);

        menuService.deleteMenu(RESTAURANT_ID, USER_ID);

        verify(menuItemRepository)
                .softDeleteAllOfMenu(eq(MENU_ID), any(OffsetDateTime.class));
        verify(menuRepository)
                .softDelete(eq(MENU_ID), any(OffsetDateTime.class));
    }

    @Test
    void deleteMenu_shouldThrowWhenMenuDoesNotExist() {
        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                MenuNotFoundException.class,
                () -> menuService.deleteMenu(RESTAURANT_ID, USER_ID)
        );

        verify(menuItemRepository, never())
                .softDeleteAllOfMenu(anyLong(), any());
        verify(menuRepository, never()).softDelete(anyLong(), any());
    }

    // ---------------------------------------------------------
    // CREATE MENU ITEM
    // ---------------------------------------------------------

    @Test
    void createMenuItem_shouldCreateItemSuccessfully() {
        CreateMenuItemRequest request = createItemRequest();

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
//        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository.existsByMenuItemCode("BURGER_01"))
                .thenReturn(false);
        when(menuMapper.toItemResponse(any(MenuItem.class)))
                .thenReturn(menuItemResponse);

        MenuItemResponse result =
                menuService.createMenuItem(RESTAURANT_ID, USER_ID, request);

        assertSame(menuItemResponse, result);

        ArgumentCaptor<MenuItem> captor =
                ArgumentCaptor.forClass(MenuItem.class);
        verify(menuItemRepository).save(captor.capture());

        MenuItem savedItem = captor.getValue();
        assertSame(menu, savedItem.getMenu());
        assertEquals("BURGER_01", savedItem.getMenuItemCode());
        assertEquals("Classic Burger", savedItem.getName());
        assertEquals(new BigDecimal("12.50"), savedItem.getItemPrice());
        assertEquals(20, savedItem.getStock());
        assertEquals("Classic beef burger", savedItem.getNote());
        assertEquals("https://example.com/burger.jpg",
                savedItem.getMenuItemImageUrl());
    }

    @Test
    void createMenuItem_shouldDefaultAvailabilityToTrueWhenNull() {
        CreateMenuItemRequest request = createItemRequest();
        request.setIsAvailable(null);

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
//        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository.existsByMenuItemCode("BURGER_01"))
                .thenReturn(false);
        when(menuMapper.toItemResponse(any(MenuItem.class)))
                .thenReturn(menuItemResponse);

        menuService.createMenuItem(RESTAURANT_ID, USER_ID, request);

        ArgumentCaptor<MenuItem> captor =
                ArgumentCaptor.forClass(MenuItem.class);
        verify(menuItemRepository).save(captor.capture());

        assertTrue(captor.getValue().isAvailable());
    }

    @Test
    void createMenuItem_shouldAllowExplicitlyUnavailableItem() {
        CreateMenuItemRequest request = createItemRequest();
        request.setIsAvailable(false);

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
//        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository.existsByMenuItemCode("BURGER_01"))
                .thenReturn(false);
        when(menuMapper.toItemResponse(any(MenuItem.class)))
                .thenReturn(menuItemResponse);

        menuService.createMenuItem(RESTAURANT_ID, USER_ID, request);

        ArgumentCaptor<MenuItem> captor =
                ArgumentCaptor.forClass(MenuItem.class);
        verify(menuItemRepository).save(captor.capture());

        assertFalse(captor.getValue().isAvailable());
    }

    @Test
    void createMenuItem_shouldThrowWhenCodeAlreadyExists() {
        CreateMenuItemRequest request = createItemRequest();

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
//        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository.existsByMenuItemCode("BURGER_01"))
                .thenReturn(true);

        assertThrows(
                MenuItemCodeInUseException.class,
                () -> menuService.createMenuItem(
                        RESTAURANT_ID, USER_ID, request)
        );

        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    void createMenuItem_shouldThrowWhenMenuDoesNotExist() {
        CreateMenuItemRequest request = createItemRequest();

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                MenuNotFoundException.class,
                () -> menuService.createMenuItem(
                        RESTAURANT_ID, USER_ID, request)
        );

        verify(menuItemRepository, never()).save(any(MenuItem.class));
        verify(menuItemRepository, never()).existsByMenuItemCode(anyString());
    }

    @Test
    void createMenuItem_shouldTranslateItemCodeConstraintViolation() {
        CreateMenuItemRequest request = createItemRequest();

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
//        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository.existsByMenuItemCode("BURGER_01"))
                .thenReturn(false);
        doThrow(constraintViolation("menu_items_menu_item_code_key"))
                .when(menuItemRepository).save(any(MenuItem.class));

        assertThrows(
                MenuItemCodeInUseException.class,
                () -> menuService.createMenuItem(
                        RESTAURANT_ID, USER_ID, request)
        );
    }

    // ---------------------------------------------------------
    // UPDATE MENU ITEM
    // ---------------------------------------------------------

    @Test
    void updateMenuItem_shouldUpdateAllProvidedFields() {
        UpdateMenuItemRequest request = new UpdateMenuItemRequest();
        request.setCode("BURGER_02");
        request.setName("Cheese Burger");
        request.setPrice(new BigDecimal("15.00"));
        request.setStock(30);
        request.setNote("Burger with cheese");
        request.setImageUrl("https://example.com/cheeseburger.jpg");

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository
                .findByIdAndMenu_IdAndMenuItemDeletedAtIsNull(
                        MENU_ITEM_ID, MENU_ID))
                .thenReturn(Optional.of(menuItem));
        when(menuItem.getId()).thenReturn(MENU_ITEM_ID);
        when(menuItemRepository.existsByMenuItemCodeAndIdNot(
                "BURGER_02", MENU_ITEM_ID))
                .thenReturn(false);
        when(menuMapper.toItemResponse(menuItem))
                .thenReturn(menuItemResponse);

        MenuItemResponse result = menuService.updateMenuItem(
                RESTAURANT_ID, MENU_ITEM_ID, USER_ID, request);

        assertSame(menuItemResponse, result);
        verify(menuItem).setMenuItemCode("BURGER_02");
        verify(menuItem).setName("Cheese Burger");
        verify(menuItem).setItemPrice(new BigDecimal("15.00"));
        verify(menuItem).setStock(30);
        verify(menuItem).setNote("Burger with cheese");
        verify(menuItem).setMenuItemImageUrl(
                "https://example.com/cheeseburger.jpg");
        verify(menuItemRepository).flush();
    }

    @Test
    void updateMenuItem_shouldLeaveNullFieldsUnchanged() {
        UpdateMenuItemRequest request = new UpdateMenuItemRequest();

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository
                .findByIdAndMenu_IdAndMenuItemDeletedAtIsNull(
                        MENU_ITEM_ID, MENU_ID))
                .thenReturn(Optional.of(menuItem));
        when(menuMapper.toItemResponse(menuItem))
                .thenReturn(menuItemResponse);

        MenuItemResponse result = menuService.updateMenuItem(
                RESTAURANT_ID, MENU_ITEM_ID, USER_ID, request);

        assertSame(menuItemResponse, result);
        verify(menuItem, never()).setMenuItemCode(anyString());
        verify(menuItem, never()).setName(anyString());
        verify(menuItem, never()).setItemPrice(any());
        verify(menuItem, never()).setStock(anyInt());
        verify(menuItem, never()).setNote(anyString());
        verify(menuItem, never()).setMenuItemImageUrl(anyString());
        verify(menuItemRepository).flush();
    }

    @Test
    void updateMenuItem_shouldThrowWhenNewCodeAlreadyExists() {
        UpdateMenuItemRequest request = new UpdateMenuItemRequest();
        request.setCode("EXISTING_CODE");

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository
                .findByIdAndMenu_IdAndMenuItemDeletedAtIsNull(
                        MENU_ITEM_ID, MENU_ID))
                .thenReturn(Optional.of(menuItem));
        when(menuItem.getId()).thenReturn(MENU_ITEM_ID);
        when(menuItemRepository.existsByMenuItemCodeAndIdNot(
                "EXISTING_CODE", MENU_ITEM_ID))
                .thenReturn(true);

        assertThrows(
                MenuItemCodeInUseException.class,
                () -> menuService.updateMenuItem(
                        RESTAURANT_ID, MENU_ITEM_ID, USER_ID, request)
        );

        verify(menuItem, never()).setMenuItemCode(anyString());
        verify(menuItemRepository, never()).flush();
    }

    @Test
    void updateMenuItem_shouldThrowWhenMenuDoesNotExist() {
        UpdateMenuItemRequest request = new UpdateMenuItemRequest();

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                MenuNotFoundException.class,
                () -> menuService.updateMenuItem(
                        RESTAURANT_ID, MENU_ITEM_ID, USER_ID, request)
        );

        verify(menuItemRepository, never())
                .findByIdAndMenu_IdAndMenuItemDeletedAtIsNull(
                        anyLong(), anyLong());
    }

    @Test
    void updateMenuItem_shouldThrowWhenItemDoesNotExist() {
        UpdateMenuItemRequest request = new UpdateMenuItemRequest();

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository
                .findByIdAndMenu_IdAndMenuItemDeletedAtIsNull(
                        MENU_ITEM_ID, MENU_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                MenuItemNotFoundException.class,
                () -> menuService.updateMenuItem(
                        RESTAURANT_ID, MENU_ITEM_ID, USER_ID, request)
        );

        verify(menuItemRepository, never()).flush();
    }

    @Test
    void updateMenuItem_shouldTranslateItemCodeConstraintViolation() {
        UpdateMenuItemRequest request = new UpdateMenuItemRequest();
        request.setCode("BURGER_02");

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository
                .findByIdAndMenu_IdAndMenuItemDeletedAtIsNull(
                        MENU_ITEM_ID, MENU_ID))
                .thenReturn(Optional.of(menuItem));
        when(menuItem.getId()).thenReturn(MENU_ITEM_ID);
        when(menuItemRepository.existsByMenuItemCodeAndIdNot(
                "BURGER_02", MENU_ITEM_ID))
                .thenReturn(false);
        doThrow(constraintViolation("menu_items_menu_item_code_key"))
                .when(menuItemRepository).flush();

        assertThrows(
                MenuItemCodeInUseException.class,
                () -> menuService.updateMenuItem(
                        RESTAURANT_ID, MENU_ITEM_ID, USER_ID, request)
        );
    }

    // ---------------------------------------------------------
    // SET MENU ITEM AVAILABILITY
    // ---------------------------------------------------------

    @Test
    void setMenuItemAvailable_shouldUpdateAvailability() {
        SetMenuItemAvailableRequest request =
                new SetMenuItemAvailableRequest();
        request.setIsAvailable(false);

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository
                .findByIdAndMenu_IdAndMenuItemDeletedAtIsNull(
                        MENU_ITEM_ID, MENU_ID))
                .thenReturn(Optional.of(menuItem));
        when(menuMapper.toItemResponse(menuItem))
                .thenReturn(menuItemResponse);

        MenuItemResponse result = menuService.setMenuItemAvailable(
                RESTAURANT_ID, MENU_ITEM_ID, USER_ID, request);

        assertSame(menuItemResponse, result);
        verify(menuItem).setAvailable(false);
        verify(menuMapper).toItemResponse(menuItem);
    }

    @Test
    void setMenuItemAvailable_shouldAllowSettingAvailabilityToTrue() {
        SetMenuItemAvailableRequest request =
                new SetMenuItemAvailableRequest();
        request.setIsAvailable(true);

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository
                .findByIdAndMenu_IdAndMenuItemDeletedAtIsNull(
                        MENU_ITEM_ID, MENU_ID))
                .thenReturn(Optional.of(menuItem));
        when(menuMapper.toItemResponse(menuItem))
                .thenReturn(menuItemResponse);

        menuService.setMenuItemAvailable(
                RESTAURANT_ID, MENU_ITEM_ID, USER_ID, request);

        verify(menuItem).setAvailable(true);
    }

    @Test
    void setMenuItemAvailable_shouldThrowWhenItemDoesNotExist() {
        SetMenuItemAvailableRequest request =
                new SetMenuItemAvailableRequest();
        request.setIsAvailable(false);

        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository
                .findByIdAndMenu_IdAndMenuItemDeletedAtIsNull(
                        MENU_ITEM_ID, MENU_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                MenuItemNotFoundException.class,
                () -> menuService.setMenuItemAvailable(
                        RESTAURANT_ID, MENU_ITEM_ID, USER_ID, request)
        );

        verifyNoInteractions(menuMapper);
    }

    // ---------------------------------------------------------
    // DELETE MENU ITEM
    // ---------------------------------------------------------

    @Test
    void deleteMenuItem_shouldSoftDeleteItem() {
        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository
                .findByIdAndMenu_IdAndMenuItemDeletedAtIsNull(
                        MENU_ITEM_ID, MENU_ID))
                .thenReturn(Optional.of(menuItem));

        menuService.deleteMenuItem(
                RESTAURANT_ID, MENU_ITEM_ID, USER_ID);

        verify(menuItem).setMenuItemDeletedAt(
                any(OffsetDateTime.class));
    }

    @Test
    void deleteMenuItem_shouldThrowWhenItemDoesNotExist() {
        when(menuRepository.findActiveByRestaurantId(RESTAURANT_ID))
                .thenReturn(Optional.of(menu));
        when(menu.getId()).thenReturn(MENU_ID);
        when(menuItemRepository
                .findByIdAndMenu_IdAndMenuItemDeletedAtIsNull(
                        MENU_ITEM_ID, MENU_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                MenuItemNotFoundException.class,
                () -> menuService.deleteMenuItem(
                        RESTAURANT_ID, MENU_ITEM_ID, USER_ID)
        );

        verify(menuItem, never())
                .setMenuItemDeletedAt(any(OffsetDateTime.class));
    }

    // ---------------------------------------------------------
    // RESTAURANT OWNERSHIP
    // ---------------------------------------------------------

    @Test
    void createMenu_shouldNotContinueWhenOwnershipCheckFails() {
        CreateMenuRequest request = createMenuRequest();

        doThrow(new RuntimeException("Not owner"))
                .when(restaurantService)
                .ensureOwnedBy(RESTAURANT_ID, USER_ID);

        assertThrows(
                RuntimeException.class,
                () -> menuService.createMenu(
                        RESTAURANT_ID, USER_ID, request)
        );

        verify(restaurantService, never())
                .findActiveRestaurant(anyLong());
        verifyNoInteractions(menuRepository);
    }

    @Test
    void updateMenu_shouldNotContinueWhenOwnershipCheckFails() {
        UpdateMenuRequest request = new UpdateMenuRequest();

        doThrow(new RuntimeException("Not owner"))
                .when(restaurantService)
                .ensureOwnedBy(RESTAURANT_ID, USER_ID);

        assertThrows(
                RuntimeException.class,
                () -> menuService.updateMenu(
                        RESTAURANT_ID, USER_ID, request)
        );

        verifyNoInteractions(menuRepository);
    }

    // ---------------------------------------------------------
    // TEST HELPERS
    // ---------------------------------------------------------

    private CreateMenuRequest createMenuRequest() {
        CreateMenuRequest request = new CreateMenuRequest();
        request.setCode("MAIN_MENU");
        request.setCategory("MAIN");
        return request;
    }

    private CreateMenuItemRequest createItemRequest() {
        CreateMenuItemRequest request = new CreateMenuItemRequest();
        request.setCode("BURGER_01");
        request.setName("Classic Burger");
        request.setPrice(new BigDecimal("12.50"));
        request.setStock(20);
        request.setNote("Classic beef burger");
        request.setImageUrl("https://example.com/burger.jpg");
        request.setIsAvailable(true);
        return request;
    }

    private DataIntegrityViolationException constraintViolation(
            String constraintName) {

        ConstraintViolationException violation =
                new ConstraintViolationException(
                        "Unique constraint violation",
                        (SQLException) null,
                        constraintName
                );

        return new DataIntegrityViolationException(
                "Database constraint violation",
                violation
        );
    }
}
