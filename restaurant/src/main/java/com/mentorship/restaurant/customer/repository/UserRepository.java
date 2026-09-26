package com.mentorship.restaurant.customer.repository;

import com.mentorship.restaurant.customer.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Case-insensitive and blind to soft-deleted users, matching the partial unique index {@code
   * uq_users_active_email} so the check and the backstop agree.
   */
  @Query(
      """
      select count(user) > 0 from User user
      where lower(user.userEmail) = lower(:email) and user.userDeletedAt is null
      """)
  boolean existsActiveByEmail(@Param("email") String email);
}
