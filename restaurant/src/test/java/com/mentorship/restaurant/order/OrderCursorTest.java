package com.mentorship.restaurant.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mentorship.restaurant.order.exception.InvalidCursorException;
import com.mentorship.restaurant.order.service.OrderCursor;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class OrderCursorTest {

  @Test
  void survivesARoundTrip() {
    OffsetDateTime createdAt = OffsetDateTime.of(2026, 9, 7, 10, 12, 33, 0, ZoneOffset.UTC);

    OrderCursor decoded = OrderCursor.decode(OrderCursor.encode(createdAt, 42L));

    assertThat(decoded.orderId()).isEqualTo(42L);
    assertThat(decoded.createdAt().toInstant()).isEqualTo(createdAt.toInstant());
  }

  @Test
  void rejectsSomethingThatIsNotBase64() {
    assertThatThrownBy(() -> OrderCursor.decode("not a cursor"))
        .isInstanceOf(InvalidCursorException.class)
        .hasMessage("Invalid cursor");
  }

  @Test
  void rejectsBase64ThatDecodesToTheWrongShape() {
    String encoded = Base64.getUrlEncoder().encodeToString("nonsense".getBytes());

    assertThatThrownBy(() -> OrderCursor.decode(encoded))
        .isInstanceOf(InvalidCursorException.class)
        .hasMessage("Invalid cursor");
  }
}
