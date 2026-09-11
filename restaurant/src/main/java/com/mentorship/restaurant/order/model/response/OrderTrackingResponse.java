package com.mentorship.restaurant.order.model.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTrackingResponse {

    private Long id;
    private Long orderId;
    private String orderStatus;
    private LocalDateTime trackingDate;
    private Long restaurantId;
}