package com.alejo.service.items;

import com.alejo.controllers.items.dto.ItemDTO;
import com.alejo.controllers.items.dto.TopClientProductsDTO;
import com.alejo.entities.items.Item;

import java.util.List;
import java.util.Optional;

public interface IItemService {

    List<Item> findPaginatedItems(int page, int size);
    List<Item> searchItems(String query);
    List<Item> findByCategories(List<Integer> categoryIds);
    Optional<Item> findById(Integer id);
    List<Item> findByUser_id(int id);
    Item save(ItemDTO item);
    void update(ItemDTO item);
    void review(ItemDTO item, double rate);
    List<Item> findItemRequests();
    void acceptItem(int id);
    void rejectItem(int id);
    List<TopClientProductsDTO> getTopClientsByProducts();
    List<Item> searchAndFilter(String query, List<Integer> categoryIds);
}
