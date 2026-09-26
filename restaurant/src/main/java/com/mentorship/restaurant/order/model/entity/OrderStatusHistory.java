package com.mentorship.restaurant.order.model.entity;

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

@Entity
@Table(name = "order_status_history")
@Getter
@Setter
@NoArgsConstructor
public class OrderStatusHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "order_status_history_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Enumerated(EnumType.STRING)
  @Column(name = "order_status_history_from", nullable = false, length = 32)
  private OrderStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "order_status_history_to", nullable = false, length = 32)
  private OrderStatus toStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "order_status_history_actor_role", nullable = false, length = 32)
  private ActorRole actorRole;

  @Column(name = "order_status_history_at", nullable = false)
  private OffsetDateTime at;
}
