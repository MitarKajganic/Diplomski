package com.mitar.dipl.model.dto.menu_item;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MenuItemDto {

    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private String menuId;

}
