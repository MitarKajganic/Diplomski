package com.mitar.dipl.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

import java.awt.*;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "menus")
@Data
public class Menu {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    private String name; // e.g., Breakfast, Lunch, Dinner

    @OneToMany(mappedBy = "menu", cascade = CascadeType.ALL)
    private Set<MenuItem> items;

}
