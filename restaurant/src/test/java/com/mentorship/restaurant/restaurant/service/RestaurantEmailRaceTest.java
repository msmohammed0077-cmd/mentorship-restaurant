package com.mentorship.restaurant.restaurant.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.mentorship.restaurant.restaurant.exception.RestaurantEmailInUseException;
import com.mentorship.restaurant.restaurant.model.entity.Restaurant;
import com.mentorship.restaurant.restaurant.model.request.CreateRestaurantRequest;
import com.mentorship.restaurant.restaurant.model.request.UpdateRestaurantRequest;
import com.mentorship.restaurant.restaurant.repository.RestaurantRepository;
import com.mentorship.restaurant.user.model.ActorRole;
import com.mentorship.restaurant.user.model.entity.User;
import com.mentorship.restaurant.user.service.UserService;
import java.sql.SQLException;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Two requests racing past the email check: the second trips uq_users_active_email. Over HTTP the
 * window cannot be hit on demand, so this is a unit test (CLAUDE.md, "Testing").
 */
@ExtendWith(MockitoExtension.class)
class RestaurantEmailRaceTest {

  private static final String EMAIL = "contact@koshary.example.com";

  @Mock private RestaurantRepository restaurantRepository;
  @Mock private UserService userService;
  @InjectMocks private RestaurantService restaurantService;

  @Test
  void createAnswers409WhenTheIndexCatchesARacingEmail() {
    CreateRestaurantRequest request = new CreateRestaurantRequest();
    request.setName("Koshary Corner");
    request.setEmail(EMAIL);
    when(userService.isEmailTaken(EMAIL)).thenReturn(false);
    when(userService.create(any(User.class)))
        .thenThrow(emailIndexViolation());

    assertThatThrownBy(() -> restaurantService.createRestaurant(ActorRole.ADMIN, request))
        .isInstanceOf(RestaurantEmailInUseException.class)
        .hasMessage("Email is already in use");
  }

  @Test
  void createRethrowsAnyOtherViolation() {
    CreateRestaurantRequest request = new CreateRestaurantRequest();
    request.setName("Koshary Corner");
    request.setEmail(EMAIL);
    DataIntegrityViolationException tooLong =
        new DataIntegrityViolationException(
            "value too long",
            new ConstraintViolationException("value too long", new SQLException(), null));
    when(userService.isEmailTaken(EMAIL)).thenReturn(false);
    when(userService.create(any(User.class))).thenThrow(tooLong);

    assertThatThrownBy(() -> restaurantService.createRestaurant(ActorRole.ADMIN, request))
        .isSameAs(tooLong);
  }

  @Test
  void updateAnswers409WhenTheIndexCatchesARacingEmail() {
    User user = User.builder().id(7L).userEmail("old@koshary.example.com").build();
    Restaurant restaurant = Restaurant.builder().id(4L).user(user).build();
    UpdateRestaurantRequest request = new UpdateRestaurantRequest();
    request.setEmail(EMAIL);
    when(restaurantRepository.findActiveById(4L)).thenReturn(Optional.of(restaurant));
    when(userService.isEmailTakenByOther(EMAIL, 7L)).thenReturn(false);
    doThrow(emailIndexViolation()).when(userService).flush();

    assertThatThrownBy(
            () -> restaurantService.updateRestaurant(4L, ActorRole.ADMIN, null, request))
        .isInstanceOf(RestaurantEmailInUseException.class)
        .hasMessage("Email is already in use");
  }

  /** What Spring raises when Postgres refuses a second active user with the same email. */
  private static DataIntegrityViolationException emailIndexViolation() {
    return new DataIntegrityViolationException(
        "duplicate key",
        new ConstraintViolationException(
            "duplicate key", new SQLException(), "uq_users_active_email"));
  }
}
