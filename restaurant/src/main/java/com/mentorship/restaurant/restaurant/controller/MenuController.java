package com.mentorship.restaurant.restaurant.controller;


import com.mentorship.restaurant.restaurant.model.request.MenuItemRequest;
import com.mentorship.restaurant.restaurant.model.request.MenuRequest;
import com.mentorship.restaurant.restaurant.model.response.MenuItemResponse;
import com.mentorship.restaurant.restaurant.model.response.MenuResponse;
import com.mentorship.restaurant.restaurant.service.MenuService;
import com.mentorship.restaurant.order.exception.TransitionNotAllowedForRoleException;
import com.mentorship.restaurant.order.model.entity.ActorRole;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/restaurants/{restaurantId}/menus")
@Tag(name = "Menu & Menu Items")
@RequiredArgsConstructor
public class MenuController {
    private final MenuService menuService;

    private void validateRole(ActorRole role) {
        if (role == ActorRole.SYSTEM) {
            throw new TransitionNotAllowedForRoleException("Role SYSTEM may not be supplied by a caller");
        }
        if (role != ActorRole.RESTAURANT) {
            throw new TransitionNotAllowedForRoleException("Role must be restaurant");
        }
    }

    @PostMapping()
    public ResponseEntity<MenuResponse> createMenu(
            @PathVariable Long restaurantId,
            @RequestParam ActorRole role,
            @Valid @RequestBody(required = false) MenuRequest request) {

        validateRole(role);
        return ResponseEntity.ok(menuService.createMenu(restaurantId, request));
    }

    @PutMapping()
    public ResponseEntity<MenuResponse> editMenu(
            @PathVariable Long restaurantId,
            @PathVariable Long menuId,
            @RequestParam ActorRole role,
            @Valid @RequestBody(required = false) MenuRequest request) {

        validateRole(role);
        return ResponseEntity.ok(menuService.editMenu(restaurantId, menuId, request));
    }

    @DeleteMapping("/{menuId}")
    public ResponseEntity<Void> deleteMenu(
            @PathVariable Long restaurantId,
            @PathVariable Long menuId,
            @RequestParam ActorRole role) {

        validateRole(role);
        menuService.deleteMenu(restaurantId, menuId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/items")
    public ResponseEntity<MenuItemResponse> createMenuItem(
            @PathVariable Long restaurantId,
            @RequestParam ActorRole role,
            @Valid @RequestBody(required = false) MenuItemRequest request) {

        validateRole(role);
        return ResponseEntity.ok(menuService.createMenuItem(restaurantId, request));
    }

    @PutMapping("/items/{menuItemId}")
    public ResponseEntity<MenuItemResponse> editMenuItem(
            @PathVariable Long restaurantId,
            @PathVariable Long menuItemId,
            @RequestParam ActorRole role,
            @Valid @RequestBody(required = false) MenuItemRequest request) {

        validateRole(role);
        return ResponseEntity.ok(menuService.editMenuItem(restaurantId, menuItemId, request));
    }

    @PutMapping("/items/{menuItemId}/availability")
    public ResponseEntity<MenuItemResponse> setMenuItemAvailability(
            @PathVariable Long restaurantId,
            @PathVariable Long menuItemId,
            @RequestParam ActorRole role,
            @Valid @RequestBody(required = false) MenuItemRequest request) {

        validateRole(role);
        return ResponseEntity.ok(menuService.setMenuItemAvailability(restaurantId, menuItemId, request));
    }

    @DeleteMapping("/items/{menuItemId}")
    public ResponseEntity<Void> deleteMenuItem(
            @PathVariable Long restaurantId,
            @PathVariable Long menuItemId,
            @RequestParam ActorRole role) {

        validateRole(role);
        menuService.deleteMenuItem(restaurantId, menuItemId);
        return ResponseEntity.noContent().build();
    }

}
