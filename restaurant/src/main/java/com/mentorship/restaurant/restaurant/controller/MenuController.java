package com.mentorship.restaurant.restaurant.controller;

import com.mentorship.restaurant.config.PublicEndpoint;
import com.mentorship.restaurant.restaurant.model.request.CreateMenuItemRequest;
import com.mentorship.restaurant.restaurant.model.request.CreateMenuRequest;
import com.mentorship.restaurant.restaurant.model.request.SetMenuItemAvailableRequest;
import com.mentorship.restaurant.restaurant.model.request.UpdateMenuItemRequest;
import com.mentorship.restaurant.restaurant.model.request.UpdateMenuRequest;
import com.mentorship.restaurant.restaurant.model.response.MenuItemResponse;
import com.mentorship.restaurant.restaurant.model.response.MenuResponse;
import com.mentorship.restaurant.restaurant.service.MenuService;
import com.mentorship.restaurant.user.model.Caller;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A restaurant's one menu, addressed through the restaurant: no endpoint takes a menu id. */
@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/menu")
@Tag(name = "Menus")
@RequiredArgsConstructor
public class MenuController {

  private final MenuService menuService;

  @PublicEndpoint
  @GetMapping
  public ResponseEntity<MenuResponse> getMenu(@PathVariable Long restaurantId) {
    return ResponseEntity.ok(menuService.getMenu(restaurantId));
  }

  @PreAuthorize("hasAuthority('menu.write')")
  @PostMapping
  public ResponseEntity<MenuResponse> createMenu(
      @AuthenticationPrincipal Caller caller,
      @PathVariable Long restaurantId,
      @Valid @RequestBody CreateMenuRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(menuService.createMenu(restaurantId, caller.userId(), request));
  }

  @PreAuthorize("hasAuthority('menu.write')")
  @PatchMapping
  public ResponseEntity<MenuResponse> updateMenu(
      @AuthenticationPrincipal Caller caller,
      @PathVariable Long restaurantId,
      @Valid @RequestBody UpdateMenuRequest request) {
    return ResponseEntity.ok(menuService.updateMenu(restaurantId, caller.userId(), request));
  }

  @PreAuthorize("hasAuthority('menu.write')")
  @DeleteMapping
  public ResponseEntity<Void> deleteMenu(
      @AuthenticationPrincipal Caller caller, @PathVariable Long restaurantId) {
    menuService.deleteMenu(restaurantId, caller.userId());
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAuthority('menu.write')")
  @PostMapping("/items")
  public ResponseEntity<MenuItemResponse> createMenuItem(
      @AuthenticationPrincipal Caller caller,
      @PathVariable Long restaurantId,
      @Valid @RequestBody CreateMenuItemRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(menuService.createMenuItem(restaurantId, caller.userId(), request));
  }

  @PreAuthorize("hasAuthority('menu.write')")
  @PatchMapping("/items/{menuItemId}")
  public ResponseEntity<MenuItemResponse> updateMenuItem(
      @AuthenticationPrincipal Caller caller,
      @PathVariable Long restaurantId,
      @PathVariable Long menuItemId,
      @Valid @RequestBody UpdateMenuItemRequest request) {
    return ResponseEntity.ok(
        menuService.updateMenuItem(restaurantId, menuItemId, caller.userId(), request));
  }

  @PreAuthorize("hasAuthority('menu.write')")
  @PutMapping("/items/{menuItemId}/available")
  public ResponseEntity<MenuItemResponse> setMenuItemAvailable(
      @AuthenticationPrincipal Caller caller,
      @PathVariable Long restaurantId,
      @PathVariable Long menuItemId,
      @Valid @RequestBody SetMenuItemAvailableRequest request) {
    return ResponseEntity.ok(
        menuService.setMenuItemAvailable(restaurantId, menuItemId, caller.userId(), request));
  }

  @PreAuthorize("hasAuthority('menu.write')")
  @DeleteMapping("/items/{menuItemId}")
  public ResponseEntity<Void> deleteMenuItem(
      @AuthenticationPrincipal Caller caller,
      @PathVariable Long restaurantId,
      @PathVariable Long menuItemId) {
    menuService.deleteMenuItem(restaurantId, menuItemId, caller.userId());
    return ResponseEntity.noContent().build();
  }
}
