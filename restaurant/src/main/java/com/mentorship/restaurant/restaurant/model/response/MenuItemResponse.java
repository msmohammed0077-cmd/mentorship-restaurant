package com.mentorship.restaurant.restaurant.model.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemResponse {

  private Long menuItemId;
  private String code;
  private String name;

  /** The menu's: #96 lists a category per item, and a restaurant's one menu carries it. */
  private String category;

  private BigDecimal price;
  private Integer stock;
  private String note;
  private String imageUrl;

  // Boxed, like RestaurantResponse.isOpen: a primitive's getter would serialise as "available".
  private Boolean isAvailable;
}
