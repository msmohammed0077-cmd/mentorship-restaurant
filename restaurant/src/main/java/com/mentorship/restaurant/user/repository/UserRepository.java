package com.mentorship.restaurant.user.repository;

import com.mentorship.restaurant.user.model.CallerRow;
import com.mentorship.restaurant.user.model.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * The user linked to a Keycloak id and, if they are one, their customer, with the stored email.
   * Soft-deleted users are not filtered: the use-cases' own soft-delete rules answer them with 404.
   */
  @Query(
      """
      select new com.mentorship.restaurant.user.model.CallerRow(
          user.id, customer.id, user.userEmail, user.userDeletedAt)
      from User user left join Customer customer on customer.user = user
      where user.userKeycloakId = :keycloakId
      """)
  Optional<CallerRow> findCallerByKeycloakId(@Param("keycloakId") UUID keycloakId);

  @Modifying
  @Query("update User user set user.userEmail = :email where user.id = :userId")
  void updateEmail(@Param("userId") Long userId, @Param("email") String email);

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

  /** Soft-deleted users count: {@code uq_users_keycloak_id} covers every row. */
  boolean existsByUserKeycloakId(UUID keycloakId);
}
