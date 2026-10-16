package com.mentorship.restaurant.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.user.model.Caller;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

/** Logging and Principal arguments read getName(): it is the stable Keycloak id, not a record. */
class CallerAuthenticationTokenTest {

  @Test
  void isNamedAfterTheTokenSubject() {
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("keycloak-id").build();

    assertThat(new CallerAuthenticationToken(new Caller(1L, 2L), jwt, List.of()).getName())
        .isEqualTo("keycloak-id");
  }
}
