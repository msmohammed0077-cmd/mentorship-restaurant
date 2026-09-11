package com.mentorship.restaurant.order.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @Column(name = "transaction_provider_code")
    private Long transactionProviderCode;

    @Column(name = "transaction_number")
    private Long transactionNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "transaction_amount", precision = 19, scale = 2)
    private BigDecimal transactionAmount;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;
}