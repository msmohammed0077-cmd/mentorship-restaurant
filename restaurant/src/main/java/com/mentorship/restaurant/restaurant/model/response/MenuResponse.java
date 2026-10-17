package com.mentorship.restaurant.restaurant.model.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuResponse {

  private Long menuId;
  private Long restaurantId;
  private String code;
  private String category;
  private List<MenuItemResponse> items;
}
