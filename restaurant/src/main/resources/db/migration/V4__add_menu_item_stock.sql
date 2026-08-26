ALTER TABLE menu_items
    ADD menu_item_stock INTEGER;

ALTER TABLE menu_items
    ADD menu_item_note VARCHAR(255);

UPDATE menu_items SET menu_item_stock = 50 WHERE menu_item_id = 1;
UPDATE menu_items SET menu_item_stock = 75 WHERE menu_item_id = 2;
UPDATE menu_items SET menu_item_stock = 60 WHERE menu_item_id = 3;
UPDATE menu_items SET menu_item_stock = 40 WHERE menu_item_id = 4;
UPDATE menu_items SET menu_item_stock = 45 WHERE menu_item_id = 5;
UPDATE menu_items SET menu_item_stock = 80 WHERE menu_item_id = 6;

UPDATE menu_items SET menu_item_note = 'Freshly prepared Egyptian kofta with rice and salad.' WHERE menu_item_id = 1;
UPDATE menu_items SET menu_item_note = 'Crispy falafel sandwich served with tahini.' WHERE menu_item_id = 2;
UPDATE menu_items SET menu_item_note = 'Traditional koshari with tomato sauce and crispy onions.' WHERE menu_item_id = 3;
UPDATE menu_items SET menu_item_note = 'Classic beef burger with house sauce.' WHERE menu_item_id = 4;
UPDATE menu_items SET menu_item_note = 'Grilled chicken burger with fresh toppings.' WHERE menu_item_id = 5;
UPDATE menu_items SET menu_item_note = 'Golden fries with sea salt.' WHERE menu_item_id = 6;

ALTER TABLE menu_items
    ALTER COLUMN menu_item_stock SET NOT NULL;
