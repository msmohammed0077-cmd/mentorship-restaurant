package com.mentorship.restaurant.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.JwtTestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Bearer-token validation (#83 part 1). Tokens are checked on a path with no endpoint: a valid one
 * gets past authentication, an invalid one is a 401. Public paths ignore the header altogether, so
 * a stale token left in Swagger UI cannot break them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class BearerTokenEndpointTest {

  private static final String HEALTH = "/api/v1/health";
  private static final String PROBE = "/api/v1/token-check";
  private static final UUID USER = UUID.randomUUID();

  @Autowired private RestTestClient client;
  @Autowired private JwtTestSupport jwts;

  @Test
  void requestWithoutTokenStillPasses() {
    client.get().uri(HEALTH).exchange().expectStatus().isOk();
  }

  @Test
  void validTokenIsAccepted() {
    withToken(PROBE, jwts.tokenFor(USER))
        .expectStatus()
        .value(status -> assertThat(status).isNotEqualTo(401));
  }

  @Test
  void expiredTokenIsRejected() {
    assertRejected(expiredToken());
  }

  @Test
  void tokenSignedWithAnotherKeyIsRejected() {
    assertRejected(jwts.signWithUnknownKey(jwts.validClaims(USER)));
  }

  @Test
  void tokenFromAnotherIssuerIsRejected() {
    assertRejected(
        jwts.sign(jwts.validClaims(USER).issuer("http://localhost:8180/realms/elsewhere")));
  }

  @Test
  void tokenForAnotherAudienceIsRejected() {
    assertRejected(jwts.sign(jwts.validClaims(USER).audience(List.of("account"))));
  }

  @Test
  void malformedTokenIsRejected() {
    assertRejected("not-a-jwt");
  }

  @Test
  void publicPathsIgnoreAnExpiredToken() {
    withToken(HEALTH, expiredToken()).expectStatus().isOk();
    withToken("/v3/api-docs", expiredToken()).expectStatus().isOk();
  }

  @Test
  void publicPathsIgnoreAMalformedToken() {
    withToken(HEALTH, "not-a-jwt").expectStatus().isOk();
  }

  private String expiredToken() {
    Instant past = Instant.now().minus(Duration.ofMinutes(10));
    return jwts.sign(
        jwts.validClaims(USER).issuedAt(past).expiresAt(past.plus(Duration.ofMinutes(5))));
  }

  private RestTestClient.ResponseSpec withToken(String uri, String token) {
    return client.get().uri(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange();
  }

  private void assertRejected(String token) {
    withToken(PROBE, token)
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .valueMatches(HttpHeaders.WWW_AUTHENTICATE, "Bearer.*");
  }
}
