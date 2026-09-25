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

  /**
   * Same check as {@link #existsActiveByEmail}, ignoring the given user, so re-sending your own
   * email on update is not a conflict.
   */
  @Query(
      """
      select count(user) > 0 from User user
      where lower(user.userEmail) = lower(:email)
        and user.userDeletedAt is null
        and user.id <> :userId
      """)
  boolean existsActiveByEmailAndIdNot(@Param("email") String email, @Param("userId") Long userId);
}
