package com.mentorship.restaurant.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mentorship.restaurant.customer.service.CustomerService;
import com.mentorship.restaurant.user.model.Caller;
import com.mentorship.restaurant.user.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Two first requests with a fresh token race to insert the same {@code user_keycloak_id}. Over HTTP
 * the loser cannot be produced on demand, so this unit test covers it (CLAUDE.md › Testing).
 */
class CallerAuthenticationConverterTest {

  private final UUID keycloakId = UUID.randomUUID();
  private final UserService userService = mock(UserService.class);
  private final CustomerService customerService = mock(CustomerService.class);
  private final CallerAuthenticationConverter converter =
      new CallerAuthenticationConverter(userService, customerService);

  @Test
  void theLoserOfARaceResolvesTheWinnersRows() {
    Caller winner = new Caller(7L, 70L);
    when(userService.findCaller(keycloakId)).thenReturn(Optional.empty(), Optional.of(winner));
    when(customerService.registerFromToken(any(), anyString(), anyString()))
        .thenThrow(new DataIntegrityViolationException("uq_users_keycloak_id"));

    assertThat(converter.convert(signUpJwt()).getPrincipal()).isEqualTo(winner);
  }

  @Test
  void aFailedInsertWithNoWinnerPropagates() {
    when(userService.findCaller(keycloakId)).thenReturn(Optional.empty());
    when(customerService.registerFromToken(any(), anyString(), anyString()))
        .thenThrow(new DataIntegrityViolationException("something else"));

    assertThatThrownBy(() -> converter.convert(signUpJwt()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private Jwt signUpJwt() {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject(keycloakId.toString())
        .claim("email", "racer@example.com")
        .claim("email_verified", true)
        .claim("name", "Racer")
        .claim(
            "resource_access",
            Map.of(
                KeycloakPermissionsConverter.PERMISSIONS_CLIENT,
                Map.of("roles", List.of(CallerAuthenticationConverter.CUSTOMER_SIGN_UP))))
        .build();
  }
}
