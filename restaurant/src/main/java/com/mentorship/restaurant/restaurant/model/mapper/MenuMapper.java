package com.mentorship.restaurant.restaurant.model.mapper;

import com.mentorship.restaurant.restaurant.model.entity.Menu;
import com.mentorship.restaurant.restaurant.model.entity.MenuItem;
import com.mentorship.restaurant.restaurant.model.response.MenuItemResponse;
import com.mentorship.restaurant.restaurant.model.response.MenuResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MenuMapper {

  public MenuResponse toResponse(Menu menu, List<MenuItem> items) {
    return new MenuResponse(
        menu.getId(),
        menu.getRestaurant().getId(),
        menu.getMenuCode(),
        menu.getMenuCategory(),
        items.stream().map(this::toItemResponse).toList());
  }

  public MenuItemResponse toItemResponse(MenuItem item) {
    return new MenuItemResponse(
        item.getId(),
        item.getMenuItemCode(),
        item.getName(),
        item.getMenu().getMenuCategory(),
        item.getItemPrice(),
        item.getStock(),
        item.getNote(),
        item.getMenuItemImageUrl(),
        item.isAvailable());
  }
}
