package com.mentorship.restaurant.restaurant.model.entity;

import com.mentorship.restaurant.restaurant.model.entity.Restaurant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "menus")
@Getter
@Setter
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "menu_code", nullable = false, length = 100, unique = true)
    private String menuCode;

    @Column(name = "menu_category", nullable = false, length = 100)
    private String menuCategory;
}
