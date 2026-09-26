package com.mentorship.restaurant.customer.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limits a string's UTF-8 encoded length. {@code @Size} counts characters, which is not enough when
 * the consumer counts bytes — BCrypt refuses passwords over 72 bytes.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
public @interface MaxUtf8Bytes {

  int value();

  String message() default "must not exceed {value} bytes";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
