package com.mentorship.restaurant.restaurant.service;

import com.mentorship.restaurant.restaurant.exception.MenuItemNotFoundException;
import com.mentorship.restaurant.restaurant.exception.OutOfStockException;
import com.mentorship.restaurant.restaurant.exception.RestaurantAccessDeniedException;
import com.mentorship.restaurant.restaurant.exception.RestaurantClosedException;
import com.mentorship.restaurant.restaurant.exception.RestaurantEmailInUseException;
import com.mentorship.restaurant.restaurant.exception.RestaurantNotFoundException;
import com.mentorship.restaurant.restaurant.exception.RestaurantOwnerLinkedException;
import com.mentorship.restaurant.restaurant.model.entity.MenuItem;
import com.mentorship.restaurant.restaurant.model.entity.Restaurant;
import com.mentorship.restaurant.restaurant.model.mapper.RestaurantMapper;
import com.mentorship.restaurant.restaurant.model.request.CreateRestaurantRequest;
import com.mentorship.restaurant.restaurant.model.request.SetRestaurantOpenRequest;
import com.mentorship.restaurant.restaurant.model.request.UpdateRestaurantRequest;
import com.mentorship.restaurant.restaurant.model.response.RestaurantResponse;
import com.mentorship.restaurant.restaurant.repository.MenuItemRepository;
import com.mentorship.restaurant.restaurant.repository.RestaurantRepository;
import com.mentorship.restaurant.user.model.entity.User;
import com.mentorship.restaurant.user.service.UserService;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Restaurants, their menu items and stock, and restaurant ownership. The public use-cases back
 * {@link com.mentorship.restaurant.restaurant.controller.RestaurantController}; the methods without
 * {@code @Transactional} serve the cart and order domains and join the caller's transaction.
 */
@Service
@RequiredArgsConstructor
public class RestaurantService {

  private static final String ACTIVE_EMAIL_INDEX = "uq_users_active_email";
  private static final String KEYCLOAK_ID_INDEX = "uq_users_keycloak_id";

  private final RestaurantRepository restaurantRepository;
  private final MenuItemRepository menuItemRepository;
  private final RestaurantMapper restaurantMapper;
  private final UserService userService;

  /**
   * A new restaurant starts closed: it has no menu yet. Its account is linked to the owner's
   * Keycloak user from the start, since login never links an existing row (ADR 0009).
   */
  @Transactional
  public RestaurantResponse createRestaurant(CreateRestaurantRequest request) {
    String email = request.getEmail().toLowerCase(Locale.ROOT);
    ensureEmailAvailable(email);
    ensureOwnerUnlinked(request.getOwnerKeycloakId());
    // @NotBlank refuses an all-blank name, but not padding around a real one.
    String name = request.getName().trim();

    User user =
        createUser(
            User.builder()
                .userName(name)
                .userEmail(email)
                .userKeycloakId(request.getOwnerKeycloakId())
                .build());

    Restaurant restaurant =
        Restaurant.builder()
            .user(user)
            .restaurantName(name)
            .restaurantDescription(request.getDescription())
            .isOpen(false)
            .build();

    return restaurantMapper.toResponse(restaurantRepository.save(restaurant));
  }

  @Transactional(readOnly = true)
  public RestaurantResponse getRestaurant(Long restaurantId) {
    return restaurantMapper.toResponse(findActiveRestaurant(restaurantId));
  }

  /**
   * The client's page and size, always in id order: a client's {@code sort} is ignored. Spring
   * clamps the size but not the page, and JPA's offset is an int, so a page that would overflow it
   * is clamped to the last one that does not (past the end either way: an empty page).
   */
  @Transactional(readOnly = true)
  public PagedModel<RestaurantResponse> listRestaurants(Pageable pageable) {
    int size = pageable.getPageSize();
    int page = Math.min(pageable.getPageNumber(), Integer.MAX_VALUE / size);
    Pageable byId = PageRequest.of(page, size, Sort.by("id"));
    return new PagedModel<>(
        restaurantRepository
            .findByUser_UserDeletedAtIsNull(byId)
            .map(restaurantMapper::toResponse));
  }

  /**
   * Absent or null fields are left as they are. The name is the restaurant's and its user's. The
   * email is Keycloak's (ADR 0010), so it is not edited here. The owner's alone: ownership is
   * checked before the restaurant is read.
   */
  @Transactional
  public RestaurantResponse updateRestaurant(
      Long restaurantId, Long callerUserId, UpdateRestaurantRequest request) {
    ensureOwnedBy(restaurantId, callerUserId);
    Restaurant restaurant = findActiveRestaurant(restaurantId);
    User user = restaurant.getUser();

    if (request.getName() != null) {
      String name = request.getName().trim();
      restaurant.setRestaurantName(name);
      user.setUserName(name);
    }
    if (request.getDescription() != null) {
      restaurant.setRestaurantDescription(request.getDescription());
    }

    // Managed entities: dirty checking issues the UPDATEs at flush, no save() needed.
    return restaurantMapper.toResponse(restaurant);
  }

  /**
   * Idempotent. Closing stops new business (add-to-cart, checkout); orders already placed carry on.
   * The owner's alone, as edit is.
   */
  @Transactional
  public RestaurantResponse setRestaurantOpen(
      Long restaurantId, Long callerUserId, SetRestaurantOpenRequest request) {
    ensureOwnedBy(restaurantId, callerUserId);
    Restaurant restaurant = findActiveRestaurant(restaurantId);

    // Lombok strips "is" from the setter of boolean isOpen: setOpen, not setIsOpen.
    restaurant.setOpen(request.getIsOpen());
    return restaurantMapper.toResponse(restaurant);
  }

  /**
   * The restaurant is the caller's, else a 403. One that does not exist is not theirs either, so a
   * caller cannot probe which ids exist. For edit, open and the order-status endpoints.
   */
  public void ensureOwnedBy(Long restaurantId, Long userId) {
    if (!restaurantRepository.existsByIdAndUser_Id(restaurantId, userId)) {
      throw new RestaurantAccessDeniedException("Restaurant belongs to another owner");
    }
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

  /**
   * Shared with {@link RestaurantDeletionService}. Unknown and deleted are both 404; joins the
   * caller's transaction.
   */
  public Restaurant findActiveRestaurant(Long restaurantId) {
    return restaurantRepository
        .findActiveById(restaurantId)
        .orElseThrow(() -> new RestaurantNotFoundException("Restaurant not found"));
  }

  private void ensureEmailAvailable(String email) {
    if (userService.isEmailTaken(email)) {
      throw new RestaurantEmailInUseException("Email is already in use");
    }
  }

  private void ensureOwnerUnlinked(UUID ownerKeycloakId) {
    if (userService.isKeycloakIdLinked(ownerKeycloakId)) {
      throw new RestaurantOwnerLinkedException("Owner account is already linked");
    }
  }

  /**
   * The unique indexes are the backstop for two requests racing past {@link #ensureEmailAvailable}
   * or {@link #ensureOwnerUnlinked}. The IDENTITY insert runs at once, so the violation surfaces
   * here.
   */
  private User createUser(User user) {
    try {
      return userService.create(user);
    } catch (DataIntegrityViolationException exception) {
      throw conflictOrRethrow(exception);
    }
  }

  /**
   * Only the active-email and Keycloak-id indexes mean a conflict. Any other violation (a column
   * too long, say) is a bug, and is rethrown to stay a 500 rather than be reported as a conflict.
   */
  private static RuntimeException conflictOrRethrow(DataIntegrityViolationException exception) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation) {
        if (ACTIVE_EMAIL_INDEX.equals(violation.getConstraintName())) {
          return new RestaurantEmailInUseException("Email is already in use");
        }
        if (KEYCLOAK_ID_INDEX.equals(violation.getConstraintName())) {
          return new RestaurantOwnerLinkedException("Owner account is already linked");
        }
      }
    }
    return exception;
  }
}
