
package com.mentorship.restaurant.menu.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemRequest {

    private Long menuId;

    @NotBlank
    private String menuItemCode;

    @NotBlank
    private String name;

    private String menuItemImageUrl;
    private String note;

    @NotNull
    @Min(0)
    private Integer stock;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal itemPrice;

    private Boolean available;
}
