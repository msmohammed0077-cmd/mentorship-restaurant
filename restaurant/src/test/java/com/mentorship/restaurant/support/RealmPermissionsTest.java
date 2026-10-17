package com.mentorship.restaurant.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
  void restaurantOwnerHoldsTheKitchenAndRestaurantPermissions() {
    assertThat(RealmPermissions.of("RESTAURANT_OWNER"))
        .containsExactlyInAnyOrder(
            "order.read",
            "order.accept",
            "order.reject",
            "order.prepare",
            "restaurant.write",
            "menu.write");
  }

  @Test
  void adminHoldsEveryPermission() {
    assertThat(RealmPermissions.all()).hasSize(19);
    assertThat(RealmPermissions.of("ADMIN")).isEqualTo(RealmPermissions.all());
  }

  /** IDEs may run tests from the repository root rather than {@code restaurant/}. */
  @Test
  void findsTheRealmFileFromRestaurantOrTheRepositoryRoot(@TempDir Path root) throws IOException {
    Path realm = root.resolve("restaurant").resolve("keycloak").resolve("realm-restaurant.json");
    Files.createDirectories(realm.getParent());
    Files.writeString(realm, "{}");

    assertThat(RealmPermissions.locate(root)).isEqualTo(realm);
    assertThat(RealmPermissions.locate(root.resolve("restaurant"))).isEqualTo(realm);
  }

  @Test
  void aMissingRealmFileNamesWhereItLooked(@TempDir Path elsewhere) {
    assertThatThrownBy(() -> RealmPermissions.locate(elsewhere))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(elsewhere.toString());
  }

  @Test
  void anUnknownRoleIsAnError() {
    assertThatThrownBy(() -> RealmPermissions.of("WIZARD"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
