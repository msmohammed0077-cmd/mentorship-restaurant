package com.mentorship.restaurant.restaurant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Create-restaurant naming a Keycloak user that another account is already linked to. */
@ResponseStatus(HttpStatus.CONFLICT)
public class RestaurantOwnerLinkedException extends RestaurantException {
  public RestaurantOwnerLinkedException(String message) {
    super(message);
  }
}
