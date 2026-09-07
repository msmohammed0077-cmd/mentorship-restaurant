package com.mentorship.restaurant.permission;

/** Permission codes. Referenced by handlers so a typo is a compile error, not a silent 403. */
public final class Permissions {

  public static final String ORDERS_READ_ORDER = "orders.read_order";

  private Permissions() {}
}
