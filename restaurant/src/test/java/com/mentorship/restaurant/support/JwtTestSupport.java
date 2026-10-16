package com.mentorship.restaurant.support;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/**
 * Signs Keycloak-shaped access tokens with the test key from {@link TestJwtConfig}. A test that
 * needs an invalid token starts from {@link #validClaims} and changes one thing.
 */
@Component
public class JwtTestSupport {

  /** A key the application does not trust, made once: RSA key generation is slow. */
  private static final NimbusJwtEncoder UNKNOWN_KEY_ENCODER =
      encoderFor(TestJwtConfig.generateRsaKeyPair());

  private final NimbusJwtEncoder encoder;
  private final String issuer;
  private final List<String> audiences;

  // Hand-written: Lombok drops @Value from generated constructor parameters.
  public JwtTestSupport(
      KeyPair testJwtKeyPair,
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
      @Value("${spring.security.oauth2.resourceserver.jwt.audiences}") List<String> audiences) {
    this.encoder = encoderFor(testJwtKeyPair);
    this.issuer = issuer;
    this.audiences = audiences;
  }

  /** A valid access token for the Keycloak user {@code keycloakId}. */
  public String tokenFor(UUID keycloakId) {
    return sign(validClaims(keycloakId));
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
    return sign(claims, encoder);
  }

  /** Signs with a key the application does not trust. */
  public String signWithUnknownKey(JwtClaimsSet.Builder claims) {
    return sign(claims, UNKNOWN_KEY_ENCODER);
  }

  private static String sign(JwtClaimsSet.Builder claims, NimbusJwtEncoder encoder) {
    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
  }

  private static NimbusJwtEncoder encoderFor(KeyPair keyPair) {
    RSAKey key =
        new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
            .privateKey(keyPair.getPrivate())
            .build();
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
  }
}
