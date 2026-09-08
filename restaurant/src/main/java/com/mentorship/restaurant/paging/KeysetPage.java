package com.mentorship.restaurant.paging;

import java.util.List;

public record KeysetPage<T>(List<T> items, boolean hasMore) {
  public static <T> KeysetPage<T> of(List<T> rows, int limit) {
    boolean hasMore = rows.size() > limit;
    return new KeysetPage<>(
        hasMore ? List.copyOf(rows.subList(0, limit)) : List.copyOf(rows), hasMore);
  }

  public T last() {
    return items.get(items.size() - 1);
  }

  public boolean isEmpty() {
    return items.isEmpty();
  }
}
