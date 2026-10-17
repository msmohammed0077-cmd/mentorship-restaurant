
package com.mentorship.restaurant.restaurant.model.response;

import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemResponse {

    private Long id;
    private Long menuId;
    private String menuItemCode;
    private String name;
    private String menuItemImageUrl;
    private String note;
    private Integer stock;
    private BigDecimal itemPrice;
    private boolean available;
}
