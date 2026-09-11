package com.mentorship.restaurant.order.model.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Long id;
    private Long transactionProviderCode;
    private Long transactionNumber;
    private Long orderId;
    private String status;
    private BigDecimal transactionAmount;
    private LocalDateTime transactionDate;
}