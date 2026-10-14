package com.mentorship.restaurant.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Pins the realm file's role compositions (ADR 0008); tests sign tokens from them. */
class RealmPermissionsTest {

  @Test
  void customerHoldsTheCustomerPermissions() {
    assertThat(RealmPermissions.of("CUSTOMER"))
        .containsExactlyInAnyOrder(
            "customer.read",
            "customer.write",
            "address.read",
            "address.write",
            "payment-method.read",
            "payment-method.write",
            "cart.read",
            "cart.write",
            "order.create",
            "order.read",
            "order.cancel",
            "order.rate");
  }

  @Test
  void restaurantOwnerHoldsTheKitchenPermissions() {
    assertThat(RealmPermissions.of("RESTAURANT_OWNER"))
        .containsExactlyInAnyOrder("order.read", "order.accept", "order.reject", "order.prepare");
  }

  @Test
  void adminHoldsEveryPermission() {
    assertThat(RealmPermissions.all()).hasSize(15);
    assertThat(RealmPermissions.of("ADMIN")).isEqualTo(RealmPermissions.all());
  }

  @Test
  void anUnknownRoleIsAnError() {
    assertThatThrownBy(() -> RealmPermissions.of("WIZARD"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
