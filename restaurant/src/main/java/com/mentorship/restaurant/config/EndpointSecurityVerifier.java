package com.mentorship.restaurant.config;

import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Deny by default for our own handlers (ADR 0008): the security chain only requires a token, so a
 * handler that forgot {@code @PreAuthorize} would be open to every logged-in user. Each of ours
 * must carry {@code @PreAuthorize} (on the method or its class) or {@link PublicEndpoint}, or the
 * application does not start.
 */
@Component
public class EndpointSecurityVerifier {

  private static final String OUR_PACKAGE = "com.mentorship.restaurant";

  private final RequestMappingHandlerMapping handlerMapping;

  // Hand-written: Lombok drops @Qualifier from generated parameters, and two mappings exist.
  public EndpointSecurityVerifier(
      @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
    this.handlerMapping = handlerMapping;
  }

  @EventListener(ContextRefreshedEvent.class)
  public void verifyOnStartup() {
    verify(handlerMapping.getHandlerMethods().values());
  }

  static void verify(Collection<HandlerMethod> handlers) {
    List<String> unguarded =
        handlers.stream()
            .filter(handler -> handler.getBeanType().getPackageName().startsWith(OUR_PACKAGE))
            .filter(handler -> !guarded(handler))
            .map(
                handler ->
                    handler.getBeanType().getSimpleName() + "." + handler.getMethod().getName())
            .sorted()
            .toList();
    if (!unguarded.isEmpty()) {
      throw new IllegalStateException(
          "Handlers without @PreAuthorize or @PublicEndpoint: " + unguarded);
    }
  }

  private static boolean guarded(HandlerMethod handler) {
    return handler.hasMethodAnnotation(PreAuthorize.class)
        || handler.hasMethodAnnotation(PublicEndpoint.class)
        || AnnotatedElementUtils.hasAnnotation(handler.getBeanType(), PreAuthorize.class);
  }
}
