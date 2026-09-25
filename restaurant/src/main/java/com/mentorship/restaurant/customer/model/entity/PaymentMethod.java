package com.mentorship.restaurant.customer.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Display metadata for a saved card. The full card number and CVV are never stored. */
@Entity
@Table(name = "payment_methods")
@Getter
@Setter
@NoArgsConstructor
public class PaymentMethod {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payment_method_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method_brand", nullable = false, length = 20)
  private CardBrand brand;

  // CHAR(4) in the schema; without the type code Hibernate expects VARCHAR and validation fails.
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "payment_method_last4", nullable = false, length = 4)
  private String last4;

  @Column(name = "payment_method_expiry_month", nullable = false)
  private Short expiryMonth;

  @Column(name = "payment_method_expiry_year", nullable = false)
  private Short expiryYear;

  @Column(name = "payment_method_holder_name", nullable = false, length = 150)
  private String holderName;

  @Column(name = "payment_method_is_default", nullable = false)
  private boolean isDefault;

  @CreationTimestamp
  @Column(name = "payment_method_created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;
}
