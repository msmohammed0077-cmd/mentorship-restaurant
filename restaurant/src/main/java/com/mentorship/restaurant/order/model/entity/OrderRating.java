package com.mentorship.restaurant.order.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_ratings")
@Getter
@Setter
@NoArgsConstructor
public class OrderRating {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "order_rating_id")
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false, unique = true)
  private Order order;

  @Column(name = "order_rating_score", nullable = false)
  private Integer score;

  @Column(name = "order_rating_comment", length = 1000)
  private String comment;

  @Column(name = "order_rating_created_at", nullable = false)
  private OffsetDateTime createdAt;
}
