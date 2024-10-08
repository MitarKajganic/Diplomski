package com.mitar.dipl.mapper;

import com.mitar.dipl.model.dto.menu_item.MenuItemCreateDto;
import com.mitar.dipl.model.dto.menu_item.MenuItemDto;
import com.mitar.dipl.model.entity.MenuItem;
import com.mitar.dipl.repository.MenuRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MenuItemMapper {

    private MenuRepository menuRepository;

    public MenuItemDto toDto(MenuItem menuItem) {
        MenuItemDto menuItemDto = new MenuItemDto();
        menuItemDto.setName(menuItem.getName());
        menuItemDto.setDescription(menuItem.getDescription());
        menuItemDto.setPrice(menuItem.getPrice().toString());
        menuItemDto.setCategory(menuItem.getCategory());
        menuItemDto.setMenuId(menuItem.getMenu().getId().toString());
        return menuItemDto;
    }

    public MenuItem toEntity(MenuItemCreateDto menuItemCreateDto) {
        MenuItem menuItem = new MenuItem();
        menuItem.setName(menuItemCreateDto.getName());
        menuItem.setDescription(menuItemCreateDto.getDescription());
        menuItem.setPrice(new BigDecimal(menuItemCreateDto.getPrice()));
        menuItem.setCategory(menuItemCreateDto.getCategory());
        if (menuItemCreateDto.getMenuId() != null)
            menuItem.setMenu(menuRepository.findById(UUID.fromString(menuItemCreateDto.getMenuId())).get());
        return menuItem;
    }
}
