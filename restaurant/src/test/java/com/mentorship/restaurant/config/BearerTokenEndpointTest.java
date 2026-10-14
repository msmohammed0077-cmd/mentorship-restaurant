package com.mentorship.restaurant.config;

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
 * Bearer tokens are validated but not yet required (#83 part 1). The endpoint is irrelevant;
 * health needs no seeded data.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class BearerTokenEndpointTest {

  private static final String HEALTH = "/api/v1/health";
  private static final UUID USER = UUID.randomUUID();

  @Autowired private RestTestClient client;
  @Autowired private JwtTestSupport jwts;

  @Test
  void requestWithoutTokenStillPasses() {
    client.get().uri(HEALTH).exchange().expectStatus().isOk();
  }

  @Test
  void validTokenIsAccepted() {
    withToken(jwts.tokenFor(USER)).expectStatus().isOk();
  }

  @Test
  void expiredTokenIsRejected() {
    Instant past = Instant.now().minus(Duration.ofMinutes(10));
    assertRejected(
        jwts.sign(
            jwts.validClaims(USER).issuedAt(past).expiresAt(past.plus(Duration.ofMinutes(5)))));
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

  private RestTestClient.ResponseSpec withToken(String token) {
    return client
        .get()
        .uri(HEALTH)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .exchange();
  }

  private void assertRejected(String token) {
    withToken(token)
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .valueMatches(HttpHeaders.WWW_AUTHENTICATE, "Bearer.*");
  }
}
