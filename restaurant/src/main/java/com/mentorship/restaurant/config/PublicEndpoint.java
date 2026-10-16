package com.mentorship.restaurant.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a handler that deliberately has no {@code @PreAuthorize}: it must also be listed in {@link
 * SecurityConfig#PUBLIC_PATHS}. {@link EndpointSecurityVerifier} refuses to start the application
 * if a handler has neither.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicEndpoint {}
