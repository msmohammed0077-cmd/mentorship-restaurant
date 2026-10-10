package com.mentorship.restaurant.restaurant.service;

import com.mentorship.restaurant.restaurant.exception.MenuItemNotFoundException;
import com.mentorship.restaurant.restaurant.exception.OutOfStockException;
import com.mentorship.restaurant.restaurant.exception.RestaurantActionNotAllowedException;
import com.mentorship.restaurant.restaurant.exception.RestaurantClosedException;
import com.mentorship.restaurant.restaurant.exception.RestaurantEmailInUseException;
import com.mentorship.restaurant.restaurant.exception.RestaurantNotFoundException;
import com.mentorship.restaurant.restaurant.model.entity.MenuItem;
import com.mentorship.restaurant.restaurant.model.entity.Restaurant;
import com.mentorship.restaurant.restaurant.model.mapper.RestaurantMapper;
import com.mentorship.restaurant.restaurant.model.request.CreateRestaurantRequest;
import com.mentorship.restaurant.restaurant.model.request.SetRestaurantOpenRequest;
import com.mentorship.restaurant.restaurant.model.request.UpdateRestaurantRequest;
import com.mentorship.restaurant.restaurant.model.response.RestaurantResponse;
import com.mentorship.restaurant.restaurant.repository.MenuItemRepository;
import com.mentorship.restaurant.restaurant.repository.RestaurantRepository;
import com.mentorship.restaurant.user.model.ActorRole;
import com.mentorship.restaurant.user.model.entity.User;
import com.mentorship.restaurant.user.service.UserService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Restaurants, and their menu items and stock. The public use-cases back {@link
 * com.mentorship.restaurant.restaurant.controller.RestaurantController}; the methods without
 * {@code @Transactional} serve the cart and order domains and join the caller's transaction.
 */
@Service
@RequiredArgsConstructor
public class RestaurantService {

  private final RestaurantRepository restaurantRepository;
  private final MenuItemRepository menuItemRepository;
  private final RestaurantMapper restaurantMapper;
  private final UserService userService;

  /** A new restaurant starts closed: it has no menu yet. Its account has no usable password. */
  @Transactional
  public RestaurantResponse createRestaurant(ActorRole role, CreateRestaurantRequest request) {
    ensureAdmin(role, "create a restaurant");

    String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
    ensureEmailAvailable(email);

    User user =
        userService.create(
            User.builder()
                .userName(request.getName())
                .userEmail(email)
                .userPassword(UserService.NO_LOGIN_PASSWORD)
                .build());

    Restaurant restaurant =
        Restaurant.builder()
            .user(user)
            .restaurantName(request.getName())
            .restaurantDescription(request.getDescription())
            .isOpen(false)
            .build();

    return restaurantMapper.toResponse(restaurantRepository.save(restaurant));
  }

  @Transactional(readOnly = true)
  public RestaurantResponse getRestaurant(Long restaurantId) {
    return restaurantMapper.toResponse(findActiveRestaurant(restaurantId));
  }

  /** The client's page and size, always in id order: a client's {@code sort} is ignored. */
  @Transactional(readOnly = true)
  public PagedModel<RestaurantResponse> listRestaurants(Pageable pageable) {
    Pageable byId = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id"));
    return new PagedModel<>(
        restaurantRepository
            .findByUser_UserDeletedAtIsNull(byId)
            .map(restaurantMapper::toResponse));
  }

  /** Absent or null fields are left as they are. The name is the restaurant's and its user's. */
  @Transactional
  public RestaurantResponse updateRestaurant(
      Long restaurantId, ActorRole role, Long callerRestaurantId, UpdateRestaurantRequest request) {
    ensureMayManage(restaurantId, role, callerRestaurantId, "edit this restaurant");
    Restaurant restaurant = findActiveRestaurant(restaurantId);
    User user = restaurant.getUser();

    if (request.getEmail() != null) {
      String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
      ensureEmailAvailableExcept(email, user.getId());
      user.setUserEmail(email);
    }
    if (request.getName() != null) {
      restaurant.setRestaurantName(request.getName());
      user.setUserName(request.getName());
    }
    if (request.getDescription() != null) {
      restaurant.setRestaurantDescription(request.getDescription());
    }

    // Managed entities: dirty checking issues the UPDATEs at flush, no save() needed.
    return restaurantMapper.toResponse(restaurant);
  }

  /**
   * Idempotent. Closing stops new business (add-to-cart, checkout); orders already placed carry on.
   */
  @Transactional
  public RestaurantResponse setRestaurantOpen(
      Long restaurantId,
      ActorRole role,
      Long callerRestaurantId,
      SetRestaurantOpenRequest request) {
    ensureMayManage(restaurantId, role, callerRestaurantId, "open or close this restaurant");
    Restaurant restaurant = findActiveRestaurant(restaurantId);

    // Lombok strips "is" from the setter of boolean isOpen: setOpen, not setIsOpen.
    restaurant.setOpen(request.getIsOpen());
    return restaurantMapper.toResponse(restaurant);
  }

  public MenuItem findMenuItem(Long menuItemId) {
    return menuItemRepository
        .findActiveById(menuItemId)
        .orElseThrow(() -> new MenuItemNotFoundException("Item not found"));
  }

  /**
   * One conditional UPDATE, so two checkouts cannot both take the last unit. It clears the
   * persistence context: re-read anything loaded before it.
   */
  public void decrementStock(Long menuItemId, Integer quantity) {
    if (menuItemRepository.decrementStockIfAvailable(menuItemId, quantity) == 0) {
      throw new OutOfStockException("Requested quantity exceeds available stock");
    }
  }

  public void restoreStock(Long menuItemId, Integer quantity) {
    menuItemRepository.incrementStock(menuItemId, quantity);
  }

  /**
   * Checkout's guard: the cart's restaurant must exist and be open. A deleted one is 404, a closed
   * one 409, as add-to-cart answers.
   */
  public void ensureOrderable(Long restaurantId) {
    if (!findActiveRestaurant(restaurantId).isOpen()) {
      throw new RestaurantClosedException("Restaurant is closed");
    }
  }

  private Restaurant findActiveRestaurant(Long restaurantId) {
    return restaurantRepository
        .findActiveById(restaurantId)
        .orElseThrow(() -> new RestaurantNotFoundException("Restaurant not found"));
  }

  private void ensureAdmin(ActorRole role, String action) {
    if (role != ActorRole.ADMIN) {
      throw new RestaurantActionNotAllowedException("Role " + role + " may not " + action);
    }
  }

  /** The admin, or the restaurant itself. A restaurant that does not say who it is is refused. */
  private void ensureMayManage(
      Long restaurantId, ActorRole role, Long callerRestaurantId, String action) {
    boolean isItself = role == ActorRole.RESTAURANT && restaurantId.equals(callerRestaurantId);
    if (role != ActorRole.ADMIN && !isItself) {
      throw new RestaurantActionNotAllowedException("Role " + role + " may not " + action);
    }
  }

  private void ensureEmailAvailable(String email) {
    if (userService.isEmailTaken(email)) {
      throw new RestaurantEmailInUseException("Email is already in use");
    }
  }

  private void ensureEmailAvailableExcept(String email, Long userId) {
    if (userService.isEmailTakenByOther(email, userId)) {
      throw new RestaurantEmailInUseException("Email is already in use");
    }
  }
}
