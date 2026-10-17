
package com.mentorship.restaurant.restaurant.service;

import com.mentorship.restaurant.restaurant.exception.RestaurantNotFoundException;
import com.mentorship.restaurant.restaurant.model.entity.Menu;
import com.mentorship.restaurant.restaurant.model.entity.MenuItem;
import com.mentorship.restaurant.restaurant.model.mapper.MenuMapper;
import com.mentorship.restaurant.restaurant.model.request.MenuItemRequest;
import com.mentorship.restaurant.restaurant.model.request.MenuRequest;
import com.mentorship.restaurant.restaurant.model.response.MenuItemResponse;
import com.mentorship.restaurant.restaurant.model.response.MenuResponse;
import com.mentorship.restaurant.restaurant.repository.MenuItemRepository;
import com.mentorship.restaurant.restaurant.repository.MenuRepository;
import com.mentorship.restaurant.restaurant.model.entity.Restaurant;
import com.mentorship.restaurant.restaurant.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuService {

    private final RestaurantRepository restaurantRepository;
    private final MenuRepository menuRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuMapper menuMapper;

    public MenuResponse createMenu(
            Long restaurantId, MenuRequest request) {

        Objects.requireNonNull(request, "Menu request is required");

        Restaurant restaurant = getRestaurant(restaurantId);

        Menu menu = menuMapper.toMenuEntity(request, restaurant);

        return menuMapper.toMenuResponse(menuRepository.save(menu));
    }

    public MenuResponse editMenu(
            Long restaurantId, Long menuId, MenuRequest request) {

        Objects.requireNonNull(request, "Menu request is required");

        Menu menu = getMenuForRestaurant(restaurantId, menuId);

        if (request.getMenuCode() != null) {
            menu.setMenuCode(request.getMenuCode());
        }

        if (request.getMenuCategory() != null) {
            menu.setMenuCategory(request.getMenuCategory());
        }

        return menuMapper.toMenuResponse(menuRepository.save(menu));
    }

    public void deleteMenu(Long restaurantId, Long menuId) {

        Menu menu = getMenuForRestaurant(restaurantId, menuId);

        menuItemRepository.findAllByMenu_Id(menuId)
                .forEach(menuItemRepository::delete);

        menuRepository.delete(menu);
    }

    public MenuItemResponse createMenuItem(
            Long restaurantId, MenuItemRequest request) {

        Objects.requireNonNull(request, "Menu item request is required");

        if (request.getMenuId() == null) {
            throw new IllegalArgumentException("Menu ID is required");
        }

        Menu menu = getMenuForRestaurant(
                restaurantId, request.getMenuId());

        MenuItem item = new MenuItem();
        item.setMenu(menu);
        item.setMenuItemCode(
                Objects.requireNonNull(request.getMenuItemCode()));
        item.setName(Objects.requireNonNull(request.getName()));
        item.setMenuItemImageUrl(request.getMenuItemImageUrl());
        item.setNote(request.getNote());
        item.setStock(Objects.requireNonNull(request.getStock()));
        item.setItemPrice(Objects.requireNonNull(request.getItemPrice()));

        if (request.getAvailable() != null) {
            item.setAvailable(request.getAvailable());
        }

        return menuMapper.toMenuItemResponse(menuItemRepository.save(item));
    }

    // 5. Edit menu item
    public MenuItemResponse editMenuItem(
            Long restaurantId,
            Long menuItemId,
            MenuItemRequest request) {

        Objects.requireNonNull(request, "Menu item request is required");

        MenuItem item = getMenuItemForRestaurant(
                restaurantId, menuItemId);

        if (request.getMenuItemCode() != null) {
            item.setMenuItemCode(request.getMenuItemCode());
        }

        if (request.getName() != null) {
            item.setName(request.getName());
        }

        if (request.getMenuItemImageUrl() != null) {
            item.setMenuItemImageUrl(request.getMenuItemImageUrl());
        }

        if (request.getNote() != null) {
            item.setNote(request.getNote());
        }

        if (request.getStock() != null) {
            item.setStock(request.getStock());
        }

        if (request.getItemPrice() != null) {
            item.setItemPrice(request.getItemPrice());
        }

        if (request.getAvailable() != null) {
            item.setAvailable(request.getAvailable());
        }

        return menuMapper.toMenuItemResponse(menuItemRepository.save(item));
    }

    public MenuItemResponse setMenuItemAvailability(
            Long restaurantId,
            Long menuItemId,
            MenuItemRequest request) {

        Objects.requireNonNull(request, "Menu item request is required");

        if (request.getAvailable() == null) {
            throw new IllegalArgumentException(
                    "Availability must be provided");
        }

        MenuItem item = getMenuItemForRestaurant(
                restaurantId, menuItemId);

        item.setAvailable(request.getAvailable());

        return menuMapper.toMenuItemResponse(menuItemRepository.save(item));
    }

    public void deleteMenuItem(
            Long restaurantId, Long menuItemId) {

        MenuItem item = getMenuItemForRestaurant(
                restaurantId, menuItemId);

        menuItemRepository.delete(item);
    }

    private Restaurant getRestaurant(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() ->
                        new RestaurantNotFoundException(
                                "Restaurant is not found"));
    }

    private Menu getMenuForRestaurant(
            Long restaurantId, Long menuId) {

        getRestaurant(restaurantId);

        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Menu not found: " + menuId));

        if (!menu.getRestaurant().getId().equals(restaurantId)) {
            throw new EntityNotFoundException(
                    "Menu does not belong to this restaurant");
        }

        return menu;
    }

    private MenuItem getMenuItemForRestaurant(
            Long restaurantId, Long menuItemId) {

        MenuItem item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Menu item not found: " + menuItemId));

        if (!item.getMenu().getRestaurant().getId()
                .equals(restaurantId)) {
            throw new EntityNotFoundException(
                    "Menu item does not belong to this restaurant");
        }

        return item;
    }


}
