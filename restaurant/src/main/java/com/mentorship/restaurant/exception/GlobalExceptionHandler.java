package com.mentorship.restaurant.exception;

import com.mentorship.restaurant.cart.exception.CartItemAlreadyExistsException;
import com.mentorship.restaurant.cart.exception.CartItemNotFoundException;
import com.mentorship.restaurant.cart.exception.CartNotFoundException;
import com.mentorship.restaurant.cart.exception.CustomerNotFoundException;
import com.mentorship.restaurant.cart.exception.DifferentRestaurantException;
import com.mentorship.restaurant.cart.exception.InvalidQuantityException;
import com.mentorship.restaurant.cart.exception.MenuItemNotFoundException;
import com.mentorship.restaurant.cart.exception.OutOfStockException;
import com.mentorship.restaurant.cart.exception.RestaurantClosedException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler({
    CartItemNotFoundException.class,
    CartItemAlreadyExistsException.class,
    CartNotFoundException.class,
    CustomerNotFoundException.class,
    MenuItemNotFoundException.class,
    InvalidQuantityException.class,
    OutOfStockException.class,
    RestaurantClosedException.class,
    DifferentRestaurantException.class
  })
  public ResponseEntity<ApiErrorResponse> handleCartExceptions(
      RuntimeException exception, HttpServletRequest request) {
    return buildResponse(statusOf(exception), exception.getMessage(), request.getRequestURI());
  }

  /**
   * Every cart exception must be listed above and here. handleGenericException catches anything
   * unlisted and turns it into a 500, and @ResponseStatus on the exception itself is ignored once
   * an advice matches.
   */
  private HttpStatus statusOf(RuntimeException exception) {
    if (exception instanceof CartItemNotFoundException
        || exception instanceof CartNotFoundException
        || exception instanceof CustomerNotFoundException
        || exception instanceof MenuItemNotFoundException) {
      return HttpStatus.NOT_FOUND;
    }
    if (exception instanceof OutOfStockException
        || exception instanceof RestaurantClosedException
        || exception instanceof DifferentRestaurantException
        || exception instanceof CartItemAlreadyExistsException) {
      return HttpStatus.CONFLICT;
    }
    return HttpStatus.BAD_REQUEST;
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
