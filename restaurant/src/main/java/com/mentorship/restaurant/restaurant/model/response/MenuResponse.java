package com.mentorship.restaurant.restaurant.model.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuResponse {

    private Long id;

    private String menuCode;

    private String menuCategory;

    private String restaurantName;
    private Long restaurantId;

}
