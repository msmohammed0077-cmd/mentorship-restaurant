package com.mentorship.restaurant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {

  /**
   * Paths that never need a token. They also ignore one: a stale token left in Swagger UI would
   * otherwise turn them into 401s.
   */
  static final RequestMatcher PUBLIC_PATHS =
      new OrRequestMatcher(
          PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui.html"),
          PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**"),
          PathPatternRequestMatcher.withDefaults().matcher("/v3/api-docs/**"),
          PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/api/v1/health"));

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // A bearer token is validated when sent, but none is required yet: #83 part 4 adds that.
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(PUBLIC_PATHS).permitAll().anyRequest().permitAll())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .bearerTokenResolver(publicPathsIgnoreTokens())
                    .jwt(Customizer.withDefaults()))
        .build();
  }

  private static BearerTokenResolver publicPathsIgnoreTokens() {
    DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
    return request -> PUBLIC_PATHS.matches(request) ? null : resolver.resolve(request);
  }
}
