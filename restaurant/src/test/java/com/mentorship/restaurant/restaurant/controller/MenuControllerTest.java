
package com.mentorship.restaurant.restaurant.controller;

import com.mentorship.restaurant.config.CallerAuthenticationToken;
import com.mentorship.restaurant.restaurant.model.request.CreateMenuItemRequest;
import com.mentorship.restaurant.restaurant.model.request.CreateMenuRequest;
import com.mentorship.restaurant.restaurant.model.request.SetMenuItemAvailableRequest;
import com.mentorship.restaurant.restaurant.model.request.UpdateMenuItemRequest;
import com.mentorship.restaurant.restaurant.model.request.UpdateMenuRequest;
import com.mentorship.restaurant.restaurant.model.entity.Restaurant;
import com.mentorship.restaurant.restaurant.repository.MenuRepository;
import com.mentorship.restaurant.restaurant.repository.RestaurantRepository;
import com.mentorship.restaurant.user.model.Caller;
import com.mentorship.restaurant.user.model.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class MenuControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("testDb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name",
                postgres::getDriverClassName);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private JsonMapper jsonMapper;

    private Restaurant restaurant;
    private Caller caller;
    private Jwt jwt;

    @BeforeEach
    void setUp() {

        restaurant = new Restaurant();

        User user = new User();
        user.setId(1L);

        restaurant.setUser(user);
        restaurant.setRestaurantName("RestaurantName");
        restaurant.setRestaurantDescription("RestaurantDescription");
        restaurant.setOpen(true);

        restaurantRepository.save(restaurant);

        caller = new Caller(1L, 1L);

        jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(UUID.randomUUID().toString())
                .build();
    }

    // ---------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------

    private CallerAuthenticationToken getAuthentication() {

        return new CallerAuthenticationToken(
                caller,
                jwt,
                List.of(new SimpleGrantedAuthority("menu.write"))
        );
    }

    private MockHttpServletRequestBuilder authenticate(
            MockHttpServletRequestBuilder request) {

        return request.with(authentication(getAuthentication()));
    }

    private void createMenuThroughApi() throws Exception {

        CreateMenuRequest request = new CreateMenuRequest();
        request.setCode("MAIN_MENU");
        request.setCategory("MAIN");

        mockMvc.perform(authenticate(
                        post("/api/v1/restaurants/{restaurantId}/menu",
                                restaurant.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request))))
                .andExpect(status().isCreated());
    }

    private Long createMenuItemThroughApi() throws Exception {

        CreateMenuItemRequest request = new CreateMenuItemRequest();
        request.setCode("BURGER_01");
        request.setName("Classic Burger");
        request.setPrice(new BigDecimal("12.50"));
        request.setStock(20);
        request.setNote("Classic beef burger");
        request.setImageUrl("https://example.com/burger.jpg");
        request.setIsAvailable(true);

        String response = mockMvc.perform(authenticate(
                        post("/api/v1/restaurants/{restaurantId}/menu/items",
                                restaurant.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return jsonMapper.readTree(response)
                .get("menu_item_id")
                .asLong();
    }

    // ---------------------------------------------------------
    // Menu tests
    // ---------------------------------------------------------

    @Test
    void getMenu() throws Exception {

        createMenuThroughApi();

        mockMvc.perform(get(
                        "/api/v1/restaurants/{restaurantId}/menu",
                        restaurant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menu_id").isNumber())
                .andExpect(jsonPath("$.restaurant_id")
                        .value(restaurant.getId()))
                .andExpect(jsonPath("$.code").value("MAIN_MENU"))
                .andExpect(jsonPath("$.category").value("MAIN"))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void createMenu() throws Exception {

        CreateMenuRequest request = new CreateMenuRequest();
        request.setCode("MAIN_MENU");
        request.setCategory("MAIN");

        mockMvc.perform(authenticate(
                        post("/api/v1/restaurants/{restaurantId}/menu",
                                restaurant.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.menu_id").isNumber())
                .andExpect(jsonPath("$.restaurant_id")
                        .value(restaurant.getId()))
                .andExpect(jsonPath("$.code").value("MAIN_MENU"))
                .andExpect(jsonPath("$.category").value("MAIN"));

        assertTrue(
                menuRepository.existsByRestaurant_IdAndMenuDeletedAtIsNull(
                        restaurant.getId()
                )
        );
    }

    @Test
    void updateMenu() throws Exception {

        createMenuThroughApi();

        UpdateMenuRequest request = new UpdateMenuRequest();
        request.setCode("UPDATED_MENU");
        request.setCategory("DESSERT");

        mockMvc.perform(authenticate(
                        patch("/api/v1/restaurants/{restaurantId}/menu",
                                restaurant.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menu_id").isNumber())
                .andExpect(jsonPath("$.restaurant_id")
                        .value(restaurant.getId()))
                .andExpect(jsonPath("$.code").value("UPDATED_MENU"))
                .andExpect(jsonPath("$.category").value("DESSERT"));
    }

    @Test
    void deleteMenu() throws Exception {

        createMenuThroughApi();

        mockMvc.perform(authenticate(
                        delete("/api/v1/restaurants/{restaurantId}/menu",
                                restaurant.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(
                        "/api/v1/restaurants/{restaurantId}/menu",
                        restaurant.getId()))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------
    // Menu item tests
    // ---------------------------------------------------------

    @Test
    void createMenuItem() throws Exception {

        createMenuThroughApi();

        CreateMenuItemRequest request = new CreateMenuItemRequest();
        request.setCode("BURGER_01");
        request.setName("Classic Burger");
        request.setPrice(new BigDecimal("12.50"));
        request.setStock(20);
        request.setNote("Classic beef burger");
        request.setImageUrl("https://example.com/burger.jpg");
        request.setIsAvailable(true);

        mockMvc.perform(authenticate(
                        post("/api/v1/restaurants/{restaurantId}/menu/items",
                                restaurant.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.menu_item_id").isNumber())
                .andExpect(jsonPath("$.code").value("BURGER_01"))
                .andExpect(jsonPath("$.name").value("Classic Burger"))
                .andExpect(jsonPath("$.price").value(12.50))
                .andExpect(jsonPath("$.stock").value(20))
                .andExpect(jsonPath("$.note")
                        .value("Classic beef burger"))
                .andExpect(jsonPath("$.image_url")
                        .value("https://example.com/burger.jpg"))
                .andExpect(jsonPath("$.is_available").value(true));
    }

    @Test
    void updateMenuItem() throws Exception {

        createMenuThroughApi();
        Long menuItemId = createMenuItemThroughApi();

        UpdateMenuItemRequest request = new UpdateMenuItemRequest();
        request.setCode("BURGER_02");
        request.setName("Cheese Burger");
        request.setPrice(new BigDecimal("15.00"));
        request.setStock(30);
        request.setNote("Burger with cheese");
        request.setImageUrl("https://example.com/cheeseburger.jpg");

        mockMvc.perform(authenticate(
                        patch("/api/v1/restaurants/{restaurantId}/menu/items/{menuItemId}",
                                restaurant.getId(), menuItemId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menu_item_id").value(menuItemId))
                .andExpect(jsonPath("$.code").value("BURGER_02"))
                .andExpect(jsonPath("$.name").value("Cheese Burger"))
                .andExpect(jsonPath("$.price").value(15.00))
                .andExpect(jsonPath("$.stock").value(30))
                .andExpect(jsonPath("$.note")
                        .value("Burger with cheese"))
                .andExpect(jsonPath("$.image_url")
                        .value("https://example.com/cheeseburger.jpg"));
    }

    @Test
    void setMenuItemAvailable() throws Exception {

        createMenuThroughApi();
        Long menuItemId = createMenuItemThroughApi();

        SetMenuItemAvailableRequest request =
                new SetMenuItemAvailableRequest();
        request.setIsAvailable(false);

        mockMvc.perform(authenticate(
                        put("/api/v1/restaurants/{restaurantId}/menu/items/{menuItemId}/available",
                                restaurant.getId(), menuItemId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menu_item_id").value(menuItemId))
                .andExpect(jsonPath("$.is_available").value(false));
    }

    @Test
    void deleteMenuItem() throws Exception {

        createMenuThroughApi();
        Long menuItemId = createMenuItemThroughApi();

        mockMvc.perform(authenticate(
                        delete("/api/v1/restaurants/{restaurantId}/menu/items/{menuItemId}",
                                restaurant.getId(), menuItemId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(
                        "/api/v1/restaurants/{restaurantId}/menu",
                        restaurant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }
}
