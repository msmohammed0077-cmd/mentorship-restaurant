package com.mentorship.restaurant.permission;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StubPermissionResolverTest {

  @Test
  void returnsThePermissionsConfiguredForTheUser() {
    PermissionProperties properties = new PermissionProperties();
    properties.setPermissions(Map.of(1L, List.of("orders.read_order")));

    assertThat(new StubPermissionResolver(properties).permissionsOf(1L))
        .containsExactly("orders.read_order");
  }

  @Test
  void treatsAnAbsentUserAsHoldingNothing() {
    PermissionProperties properties = new PermissionProperties();
    properties.setPermissions(Map.of(1L, List.of("orders.read_order")));

    assertThat(new StubPermissionResolver(properties).permissionsOf(99L)).isEmpty();
  }

  @Test
  void treatsAnEmptyConfigurationAsHoldingNothing() {
    assertThat(new StubPermissionResolver(new PermissionProperties()).permissionsOf(1L)).isEmpty();
  }
}
