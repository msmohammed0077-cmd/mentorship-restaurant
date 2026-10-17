-- Menu CRUD (#92). Menus and items are soft-deleted: order_items and cart_items reference
-- menu_items without a cascade, and past orders must keep their lines.
ALTER TABLE menus ADD COLUMN menu_deleted_at TIMESTAMPTZ;

-- One menu per restaurant (#93); a deleted one does not count, so the restaurant may start again.
CREATE UNIQUE INDEX uq_menus_active_restaurant ON menus (restaurant_id) WHERE menu_deleted_at IS NULL;

-- Availability is the owner's switch, beside stock (#100).
ALTER TABLE menu_items ADD COLUMN menu_item_is_available BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE menu_items ADD COLUMN menu_item_deleted_at TIMESTAMPTZ;
