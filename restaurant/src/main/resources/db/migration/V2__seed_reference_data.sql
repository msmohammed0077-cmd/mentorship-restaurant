INSERT INTO users (user_id, user_name, user_email, user_password) VALUES
    (1, 'Ahmed Ali', 'ahmed.ali@example.com', '$2a$10$placeholder.customer1'),
    (2, 'Mona Hassan', 'mona.hassan@example.com', '$2a$10$placeholder.customer2'),
    (3, 'Nile Kitchen', 'contact@nilekitchen.example.com', '$2a$10$placeholder.restaurant1'),
    (4, 'Burger Yard', 'contact@burgeryard.example.com', '$2a$10$placeholder.restaurant2');

INSERT INTO customers (customer_id, user_id) VALUES
    (1, 1),
    (2, 2);

INSERT INTO restaurants (restaurant_id, user_id, restaurant_name, restaurant_description) VALUES
    (1, 3, 'Nile Kitchen', 'Modern Egyptian and Mediterranean dishes.'),
    (2, 4, 'Burger Yard', 'Burgers, fries, and classic fast-casual meals.');

INSERT INTO menus (menu_id, restaurant_id, menu_code, menu_category) VALUES
    (1, 1, 'NK-MAIN', 'Main'),
    (2, 2, 'BY-MAIN', 'Main');

INSERT INTO menu_items (menu_item_id, menu_id, menu_item_code, menu_item_image_url, menu_item_price) VALUES
    (1, 1, 'NK-KOFTA-01', 'https://example.com/images/kofta-platter.jpg', 185.00),
    (2, 1, 'NK-FALAFEL-01', 'https://example.com/images/falafel-sandwich.jpg', 65.00),
    (3, 1, 'NK-KOSHARI-01', 'https://example.com/images/koshari-bowl.jpg', 55.00),
    (4, 2, 'BY-CLASSIC-01', 'https://example.com/images/classic-burger.jpg', 120.00),
    (5, 2, 'BY-CHICKEN-01', 'https://example.com/images/chicken-burger.jpg', 110.00),
    (6, 2, 'BY-FRIES-01', 'https://example.com/images/fries.jpg', 35.00);
