package com.mentorship.restaurant.user.repository;

import com.mentorship.restaurant.user.model.entity.User;
import com.mentorship.restaurant.user.model.Caller;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * The user linked to a Keycloak id and, if they are one, their customer. Soft-deleted users are
   * not filtered: the use-cases' own soft-delete rules answer them with 404.
   */
  @Query(
      """
      select new com.mentorship.restaurant.user.model.Caller(user.id, customer.id)
      from User user left join Customer customer on customer.user = user
      where user.userKeycloakId = :keycloakId
      """)
  Optional<Caller> findCallerByKeycloakId(@Param("keycloakId") UUID keycloakId);

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
