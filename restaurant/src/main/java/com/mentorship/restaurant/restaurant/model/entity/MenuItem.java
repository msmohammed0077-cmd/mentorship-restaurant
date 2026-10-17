package com.mentorship.restaurant.restaurant.model.entity;

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
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MenuItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "menu_item_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "menu_id", nullable = false)
  private Menu menu;

  @Column(name = "menu_item_code", nullable = false, unique = true, length = 100)
  private String menuItemCode;

  @Column(name = "menu_item_name", nullable = false, length = 150)
  private String name;

  @Column(name = "menu_item_image_url")
  private String menuItemImageUrl;

  @Column(name = "menu_item_note")
  private String note;

  @Column(name = "menu_item_stock", nullable = false)
  private Integer stock;

  @Column(name = "menu_item_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal itemPrice;

  /** Lombok: {@code isAvailable()} and {@code setAvailable()}. */
  @Column(name = "menu_item_is_available", nullable = false)
  private boolean isAvailable;

  @Column(name = "menu_item_deleted_at")
  private OffsetDateTime menuItemDeletedAt;
}
