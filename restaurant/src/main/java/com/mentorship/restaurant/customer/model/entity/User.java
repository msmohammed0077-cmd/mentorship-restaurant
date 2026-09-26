package com.mentorship.restaurant.customer.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long id;

  @Column(name = "user_name", nullable = false, length = 150)
  private String userName;

  @Column(name = "user_email", nullable = false, length = 255)
  private String userEmail;

  @Column(name = "user_password", nullable = false, length = 255)
  private String userPassword;

  @Column(name = "user_phone", length = 20)
  private String userPhone;

  @Column(name = "user_date_of_birth")
  private LocalDate userDateOfBirth;

  @Enumerated(EnumType.STRING)
  @Column(name = "user_gender", length = 20)
  private Gender userGender;

  @Column(name = "user_deleted_at")
  private OffsetDateTime userDeletedAt;
}
