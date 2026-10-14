package com.mentorship.restaurant.support;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Replaces Spring Boot's {@link JwtDecoder}, which would fetch Keycloak's keys, with one trusting a
 * key pair generated for the test run. The issuer and audience checks are Boot's, read from the
 * same properties. A plain {@code @Configuration} under the application's package, so component
 * scanning adds it to every test context and they stay one cached context.
 */
@Configuration
public class TestJwtConfig {

  @Bean
  KeyPair testJwtKeyPair() {
    return generateRsaKeyPair();
  }

  @Bean
  JwtDecoder jwtDecoder(
      KeyPair testJwtKeyPair,
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
      @Value("${spring.security.oauth2.resourceserver.jwt.audiences}") List<String> audiences) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withPublicKey((RSAPublicKey) testJwtKeyPair.getPublic()).build();
    decoder.setJwtValidator(
        JwtValidators.createDefaultWithValidators(
            List.of(
                new JwtIssuerValidator(issuer),
                new JwtClaimValidator<List<String>>(
                    JwtClaimNames.AUD,
                    aud -> aud != null && aud.stream().anyMatch(audiences::contains)))));
    return decoder;
  }

  static KeyPair generateRsaKeyPair() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
