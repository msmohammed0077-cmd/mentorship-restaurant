package com.mentorship.restaurant.customer.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Refuses a card whose expiry month is before the current month. A card is valid through the last
 * day of its expiry month. Class-level because it reads two fields; the violation is reported on
 * {@code expiryMonth} so the 400 names a field.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotExpiredValidator.class)
public @interface NotExpired {

  String message() default "must not be before the current month";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
