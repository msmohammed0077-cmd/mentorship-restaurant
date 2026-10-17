package com.mentorship.restaurant.restaurant.model.mapper;


import com.mentorship.restaurant.restaurant.model.entity.Menu;
import com.mentorship.restaurant.restaurant.model.entity.MenuItem;
import com.mentorship.restaurant.restaurant.model.request.MenuRequest;
import com.mentorship.restaurant.restaurant.model.response.MenuItemResponse;
import com.mentorship.restaurant.restaurant.model.response.MenuResponse;
import com.mentorship.restaurant.restaurant.model.entity.Restaurant;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class MenuMapper {

    public Menu toMenuEntity(MenuRequest request, Restaurant restaurant) {
        Menu menu = new Menu();

        menu.setMenuCode(request.getMenuCode());
        menu.setMenuCategory(request.getMenuCategory());
        menu.setRestaurant(restaurant);
        return menu;
    }

    public MenuResponse toMenuResponse(Menu menu) {
        MenuResponse menuResponse = new MenuResponse();

        menuResponse.setId(menu.getId());
        menuResponse.setMenuCategory(menu.getMenuCategory());
        menuResponse.setMenuCode(menu.getMenuCode());
        menuResponse.setRestaurantName(
                menu.getRestaurant().getRestaurantName());
        menuResponse.setRestaurantId(
                menu.getRestaurant().getId());

        return menuResponse;
    }

    public MenuItemResponse toMenuItemResponse(MenuItem item) {
        return MenuItemResponse.builder()
                .id(item.getId())
                .menuId(item.getMenu().getId())
                .menuItemCode(item.getMenuItemCode())
                .name(item.getName())
                .menuItemImageUrl(item.getMenuItemImageUrl())
                .note(item.getNote())
                .stock(item.getStock())
                .itemPrice(item.getItemPrice())
                .available(item.isAvailable())
                .build();
    }


}
