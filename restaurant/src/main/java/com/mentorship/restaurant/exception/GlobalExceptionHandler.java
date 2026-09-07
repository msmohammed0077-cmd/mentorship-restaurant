package com.mentorship.restaurant.exception;

import com.mentorship.restaurant.cart.exception.CartException;
import com.mentorship.restaurant.customer.exception.CustomerException;
import com.mentorship.restaurant.order.exception.OrderException;
import com.mentorship.restaurant.permission.PermissionDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(CartException.class)
  public ResponseEntity<ApiErrorResponse> handleCartException(
      CartException exception, HttpServletRequest request) {
    return buildResponse(statusOf(exception), exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(CustomerException.class)
  public ResponseEntity<ApiErrorResponse> handleCustomerException(
      CustomerException exception, HttpServletRequest request) {
    return buildResponse(statusOf(exception), exception.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(OrderException.class)
  public ResponseEntity<ApiErrorResponse> handleOrderException(
      OrderException exception, HttpServletRequest request) {
    return buildResponse(statusOf(exception), exception.getMessage(), request.getRequestURI());
  }

  private HttpStatus statusOf(RuntimeException exception) {
    ResponseStatus annotation =
        AnnotatedElementUtils.findMergedAnnotation(exception.getClass(), ResponseStatus.class);
    if (annotation == null) {
      log.warn("{} declares no @ResponseStatus", exception.getClass().getName());
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
    return HttpStatus.valueOf(annotation.value().value());
  }

  @ExceptionHandler(PermissionDeniedException.class)
  public ResponseEntity<ApiErrorResponse> handlePermissionDenied(
      PermissionDeniedException exception, HttpServletRequest request) {
    return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage(), request.getRequestURI());
  }

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

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
    return buildResponse(
        HttpStatus.BAD_REQUEST,
        exception.getName() + " is not a valid value",
        request.getRequestURI());
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiErrorResponse> handleMissingParameter(
      MissingServletRequestParameterException exception, HttpServletRequest request) {
    return buildResponse(
        HttpStatus.BAD_REQUEST,
        exception.getParameterName() + " is required",
        request.getRequestURI());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    log.warn(
        "Unreadable request body on {}: {}",
        request.getRequestURI(),
        exception.getClass().getSimpleName());
    return buildResponse(
        HttpStatus.BAD_REQUEST, "Request body is malformed", request.getRequestURI());
  }

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
