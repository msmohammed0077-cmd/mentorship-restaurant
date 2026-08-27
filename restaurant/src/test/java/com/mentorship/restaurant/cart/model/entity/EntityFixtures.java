package com.mentorship.restaurant.cart.model.entity;

import java.math.BigDecimal;

/**
 * Builds entities for unit tests. Lives in the entity package so it can reach the protected no-arg
 * constructors; production code never constructs Restaurant, Menu, MenuItem or Customer, so those
 * stay restricted.
 *
 * <p>These instances are in-memory only. Restaurant, Menu and MenuItem carry NOT NULL columns that
 * no entity maps (user_id, menu_code, menu_item_code), so they cannot be persisted from code.
 */
public final class EntityFixtures {

  public static Restaurant restaurant(Long id, String name, boolean open) {
    Restaurant restaurant = new Restaurant();
    restaurant.setId(id);
    restaurant.setRestaurantName(name);
    restaurant.setOpen(open);
    return restaurant;
  }

  public static Menu menu(Restaurant restaurant) {
    Menu menu = new Menu();
    menu.setRestaurant(restaurant);
    return menu;
  }

  public static MenuItem menuItem(Long id, Menu menu, String name, String price, int stock) {
    MenuItem item = new MenuItem();
    item.setId(id);
    item.setMenu(menu);
    item.setName(name);
    item.setItemPrice(new BigDecimal(price));
    item.setStock(stock);
    return item;
  }

  public static Customer customer(Long id) {
    Customer customer = new Customer();
    customer.setId(id);
    return customer;
  }

  private EntityFixtures() {}
}
