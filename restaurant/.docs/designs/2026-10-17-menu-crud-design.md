# Menu CRUD — design

**Issue:** #92 (Restaurant Menu + Menu Item CRUD), sub-issues #93–#100. #101 (menu item images) is out of scope.

**PR:** #122, by Aya, rebased onto `main` and completed in place. Her commit is kept and re-homed; the fixes, the rules below, tests, specs and authorisation follow as further commits in the same PR.

## What #122 needed

#122 was written before the restaurant CRUD and #83 landed. Besides `?role=`, it put menus in a new `menu/` domain against ADR 0003, injected `RestaurantRepository` across domains, answered missing rows and invalid input with 500s (`EntityNotFoundException`, `IllegalArgumentException`, `Objects.requireNonNull`), hard-deleted items that `order_items` and `cart_items` reference without a cascade (so any item ever carted or ordered could not be deleted), mapped `editMenu` without its `{menuId}`, had no read endpoint (#96), no tests and no specs, and claimed `V15`, which `main` already uses.

## Decisions

1. **Menus stay in the `restaurant/` domain** (ADR 0003): `MenuController`, `MenuService`, `MenuMapper`, requests and responses under `restaurant/`. The duplicate `menu/exception/Restaurant*Exception` classes go; the domain's own exceptions extend `RestaurantException`.
2. **One menu per restaurant** (#92, #93). The menu is addressed through its restaurant, so no endpoint takes a menu id. A partial unique index enforces it; a second menu is 409.
3. **Delete is soft**, for menus and items, so past orders and carts keep their lines. Deleting a menu soft-deletes its items in one statement.
4. **Availability is a flag of its own**, `menu_item_is_available`, beside stock: an owner can take an item off without zeroing its stock. Add-to-cart, modify-cart, checkout and create-order refuse an unavailable item.
5. **The menu read is public**, like the restaurant reads.
6. **Writes need `menu.write`** and ownership of the restaurant.

## Endpoints

Base `/api/v1/restaurants/{restaurantId}/menu`.

| Endpoint | Issue | Guard | Answer |
|---|---|---|---|
| `GET …/menu` | #96 | public | 200, the menu and its items |
| `POST …/menu` | #93 | `menu.write` + owner | 201 |
| `PATCH …/menu` | #94 | `menu.write` + owner | 200 |
| `DELETE …/menu` | #95 | `menu.write` + owner | 204 |
| `POST …/menu/items` | #97 | `menu.write` + owner | 201 |
| `PATCH …/menu/items/{menuItemId}` | #98 | `menu.write` + owner | 200 |
| `PUT …/menu/items/{menuItemId}/available` | #100 | `menu.write` + owner | 200 |
| `DELETE …/menu/items/{menuItemId}` | #99 | `menu.write` + owner | 204 |

### Requests

| Request | Fields |
|---|---|
| `CreateMenuRequest` | `code` required, not blank, max 100; `category` required, not blank, max 100 |
| `UpdateMenuRequest` | `code`, `category`, both optional; present means not blank, max 100 |
| `CreateMenuItemRequest` | `code` required, max 100; `name` required, max 150; `price` required, ≥ 0, 10 integer and 2 fraction digits; `stock` required, ≥ 0; `note` optional, max 255; `image_url` optional, max 2048; `is_available` optional, default true |
| `UpdateMenuItemRequest` | the same fields but `is_available`, all optional; null leaves a field as it is |
| `SetMenuItemAvailableRequest` | `is_available` required |

Shape and range are Jakarta annotations (400 before the service runs), not service checks.

### Responses

`MenuResponse`: `menu_id`, `restaurant_id`, `code`, `category`, `items` (the read; writes on the menu return it too). `MenuItemResponse`: `menu_item_id`, `code`, `name`, `category` (the menu's: #96 asks for it per item), `price`, `stock`, `note`, `image_url`, `is_available`.

The read lists every item that is not deleted, available or not, with `is_available` saying which, in id order.

## Rules

- **Order of checks on writes:** token (401) → permission (403) → `RestaurantService.ensureOwnedBy(restaurantId, caller.userId())` (403, before any read; an unknown restaurant is 403 too) → the restaurant's active menu, else 404 "Menu not found" → the item, scoped to that menu, else 404 "Item not found" → business rules (409).
- **A deleted restaurant's menu does not exist:** its owner gets 404 "Menu not found" on the menu and item writes, and 404 "Restaurant not found" on create-menu, which has no menu to look up; the read answers 404 "Restaurant not found", as `GET /restaurants/{id}` does.
- **Codes are unique across every row**, deleted ones included, as V1's `UNIQUE` constraints already are: a taken code is 409 "Menu code is already in use" / "Item code is already in use". Changing those constraints to cover active rows only is left out.
- **Second menu:** 409 "Restaurant already has a menu". After a delete the restaurant may create a new one (with a new code).
- **Delete-menu** soft-deletes the menu and its items in one `@Modifying` statement each, scoped to that menu.
- **Edits** set fields on the loaded entity; dirty checking writes them, no `save()`.

### Effect on carts and orders

- `RestaurantService.findMenuItem` (add-to-cart, modify-cart) no longer finds a deleted item or an item of a deleted menu: 404 "Item not found", as for a deleted restaurant's item.
- Add-to-cart and modify-cart refuse an unavailable item: 409 "Item is unavailable".
- Checkout and create-order refuse a cart holding a deleted line (404 "Item not found") or an unavailable one (409 "Item is unavailable"). The cart is kept. The cart's read (`viewCart`) does not validate, as it does not for closed restaurants.
- A cart line's price is the price when it was added (`cart_item_price`); editing an item's price does not change carts.

## Data model

`V18__add_menu_soft_delete_and_availability.sql`:

- `menus.menu_deleted_at TIMESTAMPTZ`; a partial unique index `uq_menus_active_restaurant` on `(restaurant_id) WHERE menu_deleted_at IS NULL`. Seeds have one menu per restaurant, so it applies cleanly.
- `menu_items.menu_item_is_available BOOLEAN NOT NULL DEFAULT TRUE`; `menu_items.menu_item_deleted_at TIMESTAMPTZ`.

No ids are seeded, so no identity resync. #122's `V15__add_menu_item_availability.sql` is replaced by V18.

`Menu` and `MenuItem` become entities the application creates: `@Builder` with the private all-args and protected no-args constructors (ADR 0005, `CLAUDE.md`). `MenuItem.isAvailable` is a `Boolean`-mapped `boolean` named so Lombok's getter and setter read right (`isAvailable()` / `setAvailable()`).

## Code shape

- `MenuController` → `MenuService` (one per controller, `@Transactional` per use-case, `readOnly` for the read). `MenuService` injects `RestaurantService` for `ensureOwnedBy` and `findActiveRestaurant`, never the reverse; `RestaurantService` keeps `findMenuItem` and the stock methods other domains call.
- `MenuRepository`: the restaurant's active menu, the existence check for a second one, code checks, the soft delete. `MenuItemRepository`: the item scoped to an active menu, code checks, the menu's items for the read, the items' soft delete.
- New exceptions under `restaurant/exception/`, extending `RestaurantException`: `MenuNotFoundException` (404), `MenuAlreadyExistsException` (409), `MenuCodeInUseException` (409), `MenuItemCodeInUseException` (409), `MenuItemUnavailableException` (409).

## Authorisation

- `menu.write`, a client role of `restaurant-api`, in the composites of `RESTAURANT_OWNER` and `ADMIN`. The admin holds it but owns no restaurant, so ownership refuses it, as for restaurant edits.
- The read is `@PublicEndpoint` and in `SecurityConfig.PUBLIC_PATHS` (`GET` only).
- Each endpoint gets a row in `EndpointAuthorizationTest`; `RealmPermissionsTest` gains the permission.

## Testing

End to end, one class per use-case: `GetMenuEndpointTest`, `CreateMenuEndpointTest`, `UpdateMenuEndpointTest`, `DeleteMenuEndpointTest`, `CreateMenuItemEndpointTest`, `UpdateMenuItemEndpointTest`, `SetMenuItemAvailableEndpointTest`, `DeleteMenuItemEndpointTest`, on `RestaurantEndpointTestSupport` (restaurants and menus seeded through SQL, `asRestaurantOwner`, `asAdmin`). Each covers the happy path, 401, 403 without the permission, 403 for another owner and the admin, 404s, 409s and validation. The cart and order tests gain the unavailable and deleted item cases. `MenuRaceTest`, a unit test, covers the unique-index backstops (`uq_menus_active_restaurant`, `menus_menu_code_key`, `menu_items_menu_item_code_key`), which HTTP cannot hit on demand.

## Documentation

Specs under `restaurant/.docs/use-cases/menu-management/<use-case>/`, one per endpoint, and a "Menu Management" section in `use-cases.md` under #92. `CLAUDE.md` and ADR 0005 move `Menu` and `MenuItem` out of the "never created" list.

## Left out

- Images (#101). `image_url` stays a plain optional URL.
- Several menus per restaurant, and menu categories as their own table.
- Partial unique indexes on codes (reusing a deleted menu's or item's code).
- Restoring a deleted menu or item.
