package com.mentorship.restaurant.support;

import static com.mentorship.restaurant.config.KeycloakPermissionsConverter.PERMISSIONS_CLIENT;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Signs Keycloak-shaped access tokens with the test key from {@link TestJwtConfig}. A test that
 * needs an invalid token starts from {@link #validClaims} and changes one thing.
 */
@Component
public class JwtTestSupport {

  /** The realm's admin@example.com; V16 gives it a users row with no customer or restaurant. */
  public static final UUID ADMIN = UUID.fromString("00000000-0000-4000-a000-000000000001");

  private final KeyPair keyPair;
  private final String issuer;
  private final List<String> audiences;
  private final JdbcTemplate jdbcTemplate;

  // Hand-written: Lombok drops @Value from generated constructor parameters.
  public JwtTestSupport(
      KeyPair testJwtKeyPair,
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
      @Value("${spring.security.oauth2.resourceserver.jwt.audiences}") List<String> audiences,
      JdbcTemplate jdbcTemplate) {
    this.keyPair = testJwtKeyPair;
    this.issuer = issuer;
    this.audiences = audiences;
    this.jdbcTemplate = jdbcTemplate;
  }

  /** A valid access token for the Keycloak user {@code keycloakId}. */
  public String tokenFor(UUID keycloakId) {
    return sign(validClaims(keycloakId));
  }

  /** A valid token holding what the realm file gives {@code realmRole}, as Keycloak expands it. */
  public String tokenFor(UUID keycloakId, String realmRole) {
    return tokenWithPermissions(keycloakId, RealmPermissions.of(realmRole));
  }

  /** A valid token whose {@code resource_access.restaurant-api.roles} is {@code permissions}. */
  public String tokenWithPermissions(UUID keycloakId, Collection<String> permissions) {
    return sign(
        validClaims(keycloakId)
            .claim(
                "resource_access",
                Map.of(PERMISSIONS_CLIENT, Map.of("roles", List.copyOf(permissions)))));
  }

  /**
   * A token for a Keycloak user with no users row yet, as Keycloak issues it after sign-up: the
   * CUSTOMER permissions (the realm's default role), a verified email and a name.
   */
  public String signUpTokenFor(UUID keycloakId, String email, String name) {
    return sign(signUpClaims(keycloakId, email, name));
  }

  /** {@link #signUpTokenFor}'s claims, for a test that changes one thing. */
  public JwtClaimsSet.Builder signUpClaims(UUID keycloakId, String email, String name) {
    return validClaims(keycloakId)
        .claim(
            "resource_access",
            Map.of(
                PERMISSIONS_CLIENT,
                Map.of("roles", List.copyOf(RealmPermissions.of("CUSTOMER")))))
        .claim("email", email)
        .claim("email_verified", true)
        .claim("name", name);
  }

  /** {@code client}, sending a {@code realmRole} token for the Keycloak user {@code keycloakId}. */
  public RestTestClient asUser(RestTestClient client, UUID keycloakId, String realmRole) {
    String token = tokenFor(keycloakId, realmRole);
    return client.mutate().defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
  }

  /** {@code client}, acting as the customer: a CUSTOMER token with their user's Keycloak id. */
  public RestTestClient asCustomer(RestTestClient client, long customerId) {
    return asUser(
        client,
        keycloakIdOf(
            """
            SELECT u.user_keycloak_id FROM users u
            JOIN customers c ON c.user_id = u.user_id WHERE c.customer_id = ?
            """,
            customerId),
        "CUSTOMER");
  }

  /** {@code client}, acting as the restaurant's owner: a RESTAURANT_OWNER token. */
  public RestTestClient asRestaurantOwner(RestTestClient client, long restaurantId) {
    return asUser(
        client,
        keycloakIdOf(
            """
            SELECT u.user_keycloak_id FROM users u
            JOIN restaurants r ON r.user_id = u.user_id WHERE r.restaurant_id = ?
            """,
            restaurantId),
        "RESTAURANT_OWNER");
  }

  private UUID keycloakIdOf(String sql, long id) {
    UUID keycloakId = jdbcTemplate.queryForObject(sql, UUID.class, id);
    if (keycloakId == null) {
      throw new IllegalStateException("No Keycloak id linked to " + id);
    }
    return keycloakId;
  }

  /** The claims of a valid token, expiring in five minutes. */
  public JwtClaimsSet.Builder validClaims(UUID keycloakId) {
    Instant now = Instant.now();
    return JwtClaimsSet.builder()
        .issuer(issuer)
        .audience(audiences)
        .subject(keycloakId.toString())
        .issuedAt(now)
        .expiresAt(now.plus(Duration.ofMinutes(5)));
  }

  public String sign(JwtClaimsSet.Builder claims) {
    return sign(claims, keyPair);
  }

  /** Signs with a fresh key the application does not trust. */
  public String signWithUnknownKey(JwtClaimsSet.Builder claims) {
    return sign(claims, TestJwtConfig.generateRsaKeyPair());
  }

  private static String sign(JwtClaimsSet.Builder claims, KeyPair keyPair) {
    RSAKey key =
        new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
            .privateKey(keyPair.getPrivate())
            .build();
    NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
  }
}
