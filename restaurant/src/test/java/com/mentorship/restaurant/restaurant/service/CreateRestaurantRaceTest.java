package com.mentorship.restaurant.restaurant.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mentorship.restaurant.restaurant.exception.RestaurantEmailInUseException;
import com.mentorship.restaurant.restaurant.exception.RestaurantOwnerLinkedException;
import com.mentorship.restaurant.restaurant.model.request.CreateRestaurantRequest;
import com.mentorship.restaurant.restaurant.repository.RestaurantRepository;
import com.mentorship.restaurant.user.model.entity.User;
import com.mentorship.restaurant.user.service.UserService;
import java.sql.SQLException;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Two requests racing past the email or owner check: the second trips uq_users_active_email or
 * uq_users_keycloak_id. Over HTTP the window cannot be hit on demand, so this is a unit test
 * (CLAUDE.md, "Testing").
 */
@ExtendWith(MockitoExtension.class)
class CreateRestaurantRaceTest {

  private static final String EMAIL = "contact@koshary.example.com";
  private static final UUID OWNER = UUID.fromString("00000000-0000-4000-8000-0000000000aa");

  @Mock private RestaurantRepository restaurantRepository;
  @Mock private UserService userService;
  @InjectMocks private RestaurantService restaurantService;

  @Test
  void createAnswers409WhenTheIndexCatchesARacingEmail() {
    CreateRestaurantRequest request = new CreateRestaurantRequest();
    request.setName("Koshary Corner");
    request.setEmail(EMAIL);
    request.setOwnerKeycloakId(OWNER);
    when(userService.isEmailTaken(EMAIL)).thenReturn(false);
    when(userService.create(any(User.class))).thenThrow(emailIndexViolation());

    assertThatThrownBy(() -> restaurantService.createRestaurant(request))
        .isInstanceOf(RestaurantEmailInUseException.class)
        .hasMessage("Email is already in use");
  }

  @Test
  void createRethrowsAnyOtherViolation() {
    CreateRestaurantRequest request = new CreateRestaurantRequest();
    request.setName("Koshary Corner");
    request.setEmail(EMAIL);
    request.setOwnerKeycloakId(OWNER);
    DataIntegrityViolationException tooLong =
        new DataIntegrityViolationException(
            "value too long",
            new ConstraintViolationException("value too long", new SQLException(), null));
    when(userService.isEmailTaken(EMAIL)).thenReturn(false);
    when(userService.create(any(User.class))).thenThrow(tooLong);

    assertThatThrownBy(() -> restaurantService.createRestaurant(request)).isSameAs(tooLong);
  }

  @Test
  void createAnswers409WhenTheIndexCatchesARacingOwner() {
    CreateRestaurantRequest request = new CreateRestaurantRequest();
    request.setName("Koshary Corner");
    request.setEmail(EMAIL);
    request.setOwnerKeycloakId(OWNER);
    when(userService.isEmailTaken(EMAIL)).thenReturn(false);
    when(userService.isKeycloakIdLinked(OWNER)).thenReturn(false);
    when(userService.create(any(User.class))).thenThrow(violationOf("uq_users_keycloak_id"));

    assertThatThrownBy(() -> restaurantService.createRestaurant(request))
        .isInstanceOf(RestaurantOwnerLinkedException.class)
        .hasMessage("Owner account is already linked");
  }

  /** What Spring raises when Postgres refuses a second active user with the same email. */
  private static DataIntegrityViolationException emailIndexViolation() {
    return violationOf("uq_users_active_email");
  }

  private static DataIntegrityViolationException violationOf(String constraint) {
    return new DataIntegrityViolationException(
        "duplicate key",
        new ConstraintViolationException("duplicate key", new SQLException(), constraint));
  }
}
