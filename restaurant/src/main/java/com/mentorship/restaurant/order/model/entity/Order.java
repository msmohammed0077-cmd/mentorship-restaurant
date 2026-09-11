package com.mentorship.restaurant.order.model.entity;

import com.mentorship.restaurant.cart.model.entity.Restaurant;
import com.mentorship.restaurant.customer.model.entity.Address;
import com.mentorship.restaurant.customer.model.entity.Customer;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "order_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "order_created_at", nullable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "order_prep_time_minutes")
    private Integer prepTimeMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_rejection_reason", length = 32)
    private RejectionReason rejectionReason;

    @Column(name = "order_rejection_note")
    private String rejectionNote;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "transaction_id", unique = true)
    private Transaction transaction;

    @Column(name = "customer_note")
    private String customerNote;
}
