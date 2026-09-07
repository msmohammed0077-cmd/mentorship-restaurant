package com.mentorship.restaurant.order.service;

import com.mentorship.restaurant.order.exception.InvalidCursorException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

/**
 * The keyset position: (created_at, order_id). Composite because order_created_at is not unique —
 * two orders can share a timestamp, and the id is what keeps a page boundary stable when they do.
 *
 * <p>Base64 so clients cannot depend on its shape. It is not a secret and is not signed.
 */
public record OrderCursor(OffsetDateTime createdAt, Long orderId) {

  public static String encode(OffsetDateTime createdAt, Long orderId) {
    String raw = createdAt.toInstant().toEpochMilli() + "|" + orderId;
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  public static OrderCursor decode(String cursor) {
    try {
      String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
      String[] parts = raw.split("\\|");
      if (parts.length != 2) {
        throw new InvalidCursorException("Invalid cursor");
      }
      OffsetDateTime createdAt =
          OffsetDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(parts[0])), ZoneOffset.UTC);
      return new OrderCursor(createdAt, Long.parseLong(parts[1]));
    } catch (IllegalArgumentException exception) {
      // NumberFormatException extends IllegalArgumentException, so a non-numeric
      // part is caught by the same clause as bad base64.
      throw new InvalidCursorException("Invalid cursor");
    }
  }
}
