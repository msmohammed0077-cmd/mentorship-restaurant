package com.mentorship.restaurant.customer.model.validation;

import com.mentorship.restaurant.customer.model.request.AddPaymentMethodRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.YearMonth;

/**
 * Passes when either field is missing or the month is out of range: the field constraints report
 * those, and this one only judges a well-formed date. {@code YearMonth.now()} uses the server's
 * default time zone.
 */
public class NotExpiredValidator
    implements ConstraintValidator<NotExpired, AddPaymentMethodRequest> {

  @Override
  public boolean isValid(AddPaymentMethodRequest request, ConstraintValidatorContext context) {
    if (request == null) {
      return true;
    }
    Short year = request.getExpiryYear();
    Short month = request.getExpiryMonth();
    if (year == null || month == null || month < 1 || month > 12) {
      return true;
    }
    if (!YearMonth.of(year, month).isBefore(YearMonth.now())) {
      return true;
    }

    // GlobalExceptionHandler formats field errors only, so attach the violation to a field.
    context.disableDefaultConstraintViolation();
    context
        .buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
        .addPropertyNode("expiryMonth")
        .addConstraintViolation();
    return false;
  }
}
