package com.mentorship.restaurant.menu.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuRequest {


    private String menuCode;

    @NotNull
    private String menuCategory;
}
