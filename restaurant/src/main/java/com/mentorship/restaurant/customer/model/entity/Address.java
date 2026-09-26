package com.mentorship.restaurant.customer.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
public class Address {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "address_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Column(name = "address_label", nullable = false, length = 100)
  private String label;

  @Column(name = "address_line", nullable = false, length = 255)
  private String line;

  @Column(name = "address_city", nullable = false, length = 100)
  private String city;

  @Column(name = "address_area", nullable = false, length = 100)
  private String area;

  @Column(name = "address_note")
  private String note;

  @Column(name = "address_is_default", nullable = false)
  private boolean isDefault;

  @CreationTimestamp
  @Column(name = "address_created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;
}
