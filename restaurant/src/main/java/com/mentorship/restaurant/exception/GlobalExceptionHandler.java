package com.mentorship.restaurant.exception;

import com.mentorship.restaurant.cart.exception.CartException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Handles every CartException. Extending CartException is all a new exception has to do, so
   * there is no list here to forget to update.
   */
  @ExceptionHandler(CartException.class)
  public ResponseEntity<ApiErrorResponse> handleCartException(
      CartException exception, HttpServletRequest request) {
    return buildResponse(statusOf(exception), exception.getMessage(), request.getRequestURI());
  }

  /**
   * Reads the status the exception declares with @ResponseStatus. findMergedAnnotation searches
   * the type hierarchy, so a subclass inherits its parent's status even though @ResponseStatus is
   * not itself @Inherited.
   *
   * <p>A missing annotation falls back to 500 rather than something plausible like 400, so the
   * omission is loud the first time the exception is thrown instead of quietly returning a wrong
   * status forever.
   */
  private HttpStatus statusOf(CartException exception) {
    ResponseStatus annotation =
        AnnotatedElementUtils.findMergedAnnotation(exception.getClass(), ResponseStatus.class);
    if (annotation == null) {
      log.warn("{} declares no @ResponseStatus", exception.getClass().getName());
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
    return HttpStatus.valueOf(annotation.value().value());
  }

  /** Reports every invalid field, so one round trip tells the caller everything that is wrong. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationException(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
            .collect(Collectors.joining(", "));

    return buildResponse(
        HttpStatus.BAD_REQUEST,
        message.isEmpty() ? "Validation failed" : message,
        request.getRequestURI());
  }

  /**
   * The message is deliberately fixed: an unhandled exception's own message can carry SQL, class
   * names or connection details, none of which belong in a response. The detail goes to the log.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGenericException(
      Exception exception, HttpServletRequest request) {
    log.error("Unhandled exception on {}", request.getRequestURI(), exception);
    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request.getRequestURI());
  }

  private ResponseEntity<ApiErrorResponse> buildResponse(
      HttpStatus status, String message, String path) {
    ApiErrorResponse body =
        new ApiErrorResponse(
            OffsetDateTime.now(), status.value(), status.getReasonPhrase(), message, path);
    return ResponseEntity.status(status).body(body);
  }
}
