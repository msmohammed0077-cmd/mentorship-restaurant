package com.mentorship.restaurant.order.model.entity;

import com.mentorship.restaurant.cart.model.entity.MenuItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "order_item_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "menu_item_id", nullable = false)
  private MenuItem menuItem;

  @Column(name = "order_item_name", nullable = false, length = 150)
  private String itemName;

  @Column(name = "order_item_quantity", nullable = false)
  private Integer quantity;

  @Column(name = "order_item_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal itemPrice;

}
