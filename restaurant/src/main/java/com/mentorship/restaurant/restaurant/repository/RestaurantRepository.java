package com.mentorship.restaurant.restaurant.repository;

import com.mentorship.restaurant.restaurant.model.entity.Restaurant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

  /**
   * Soft-deleted restaurants do not exist to the API. The user is join-fetched because {@code
   * RestaurantMapper} reads the email from it and open-in-view is off.
   */
  @Query(
      """
      select restaurant from Restaurant restaurant
      join fetch restaurant.user user
      where restaurant.id = :restaurantId and user.userDeletedAt is null
      """)
  Optional<Restaurant> findActiveById(@Param("restaurantId") Long restaurantId);

  /**
   * Active restaurants, a page at a time. The user is fetched for the email; Spring derives the
   * count query.
   */
  @EntityGraph(attributePaths = "user")
  Page<Restaurant> findByUser_UserDeletedAtIsNull(Pageable pageable);
}
