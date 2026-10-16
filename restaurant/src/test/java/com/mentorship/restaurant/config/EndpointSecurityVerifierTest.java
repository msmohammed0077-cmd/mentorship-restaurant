package com.mentorship.restaurant.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;

/** A handler with neither {@code @PreAuthorize} nor {@code @PublicEndpoint} stops the app. */
class EndpointSecurityVerifierTest {

  @Test
  void refusesAnUnguardedHandler() {
    assertThatThrownBy(() -> EndpointSecurityVerifier.verify(List.of(handler("unguarded"))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Handlers.unguarded");
  }

  @Test
  void acceptsGuardedAndPublicHandlers() {
    assertThatCode(
            () ->
                EndpointSecurityVerifier.verify(
                    List.of(handler("guarded"), handler("open"), classGuarded())))
        .doesNotThrowAnyException();
  }

  private static HandlerMethod handler(String name) {
    try {
      return new HandlerMethod(new Handlers(), Handlers.class.getMethod(name));
    } catch (NoSuchMethodException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static HandlerMethod classGuarded() {
    try {
      return new HandlerMethod(new ClassGuarded(), ClassGuarded.class.getMethod("anything"));
    } catch (NoSuchMethodException exception) {
      throw new IllegalStateException(exception);
    }
  }

  static class Handlers {
    public void unguarded() {}

    @PreAuthorize("hasAuthority('x')")
    public void guarded() {}

    @PublicEndpoint
    public void open() {}
  }

  @PreAuthorize("hasAuthority('x')")
  static class ClassGuarded {
    public void anything() {}
  }
}
