package com.mentorship.restaurant.order.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_tracking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_tracking_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "order_status", nullable = false)
    private String orderStatus;

    @Column(name = "order_tracking_date", nullable = false)
    private LocalDateTime trackingDate;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;
}