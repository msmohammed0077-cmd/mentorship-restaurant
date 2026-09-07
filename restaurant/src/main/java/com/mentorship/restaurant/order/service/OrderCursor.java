package com.mentorship.restaurant.order.service;

import com.mentorship.restaurant.order.exception.InvalidCursorException;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

/**
 * The keyset position: (created_at, order_id). Composite because order_created_at is not unique —
 * two orders can share a timestamp, and the id is what keeps a page boundary stable when they do.
 *
 * <p><b>Encoded as seconds and nanoseconds, not milliseconds.</b> {@code TIMESTAMPTZ} is
 * microsecond-precision; a millisecond cursor rounds the boundary row's timestamp <i>down</i>, and
 * the {@code <} comparison then skips every order in the same millisecond. Those orders appear on
 * no page at all — a silent, permanent gap in a customer's history.
 *
 * <p>Base64 so clients cannot depend on its shape. It is not a secret and is not signed, so
 * decoding must treat every part as hostile.
 */
public record OrderCursor(OffsetDateTime createdAt, Long orderId) {

  /** Postgres {@code timestamptz} tops out at year 294276; stay far inside it. */
  private static final int MIN_YEAR = 1;

  private static final int MAX_YEAR = 9999;

  public static String encode(OffsetDateTime createdAt, Long orderId) {
    Instant instant = createdAt.toInstant();
    String raw = instant.getEpochSecond() + "|" + instant.getNano() + "|" + orderId;
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  public static OrderCursor decode(String cursor) {
    try {
      String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
      String[] parts = raw.split("\\|");
      if (parts.length != 3) {
        throw new InvalidCursorException("Invalid cursor");
      }
      OffsetDateTime createdAt =
          OffsetDateTime.ofInstant(
              Instant.ofEpochSecond(Long.parseLong(parts[0]), Long.parseLong(parts[1])),
              ZoneOffset.UTC);
      // A value the database cannot bind must not reach it: an out-of-range
      // timestamp surfaced as a 500 from Postgres rather than a 400 from here.
      if (createdAt.getYear() < MIN_YEAR || createdAt.getYear() > MAX_YEAR) {
        throw new InvalidCursorException("Invalid cursor");
      }
      return new OrderCursor(createdAt, Long.parseLong(parts[2]));
    } catch (IllegalArgumentException | DateTimeException | ArithmeticException exception) {
      // NumberFormatException extends IllegalArgumentException, so bad base64 and
      // a non-numeric part share a clause. DateTimeException and ArithmeticException
      // cover the extremes: Instant overflows before it can be compared.
      throw new InvalidCursorException("Invalid cursor");
    }
  }
}
