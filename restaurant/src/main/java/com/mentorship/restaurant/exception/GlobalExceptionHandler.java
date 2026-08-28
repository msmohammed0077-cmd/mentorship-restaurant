package com.mentorship.restaurant.exception;

import com.mentorship.restaurant.cart.exception.CartItemAlreadyExistsException;
import com.mentorship.restaurant.cart.exception.CartItemNotFoundException;
import com.mentorship.restaurant.cart.exception.CustomerNotFoundException;
import com.mentorship.restaurant.cart.exception.DifferentRestaurantException;
import com.mentorship.restaurant.cart.exception.InvalidQuantityException;
import com.mentorship.restaurant.cart.exception.MenuItemNotFoundException;
import com.mentorship.restaurant.cart.exception.OutOfStockException;
import com.mentorship.restaurant.cart.exception.RestaurantClosedException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler({
    CartItemNotFoundException.class,
    CartItemAlreadyExistsException.class,
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

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationException(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
            .orElse("Validation failed");

    return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGenericException(
      Exception exception, HttpServletRequest request) {
    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request.getRequestURI());
  }

  private ResponseEntity<ApiErrorResponse> buildResponse(
      HttpStatus status, String message, String path) {
    ApiErrorResponse body =
        new ApiErrorResponse(
            OffsetDateTime.now(), status.value(), status.getReasonPhrase(), message, path);
    return ResponseEntity.status(status).body(body);
  }
}
