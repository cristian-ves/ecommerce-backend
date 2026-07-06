package com.alejo.service.items.impl;

import com.alejo.controllers.items.dto.CategoryDTO;
import com.alejo.controllers.items.dto.ItemDTO;
import com.alejo.controllers.items.dto.TopClientProductsDTO;
import com.alejo.entities.auth.User;
import com.alejo.entities.items.Category;
import com.alejo.entities.items.Item;
import com.alejo.persistence.auth.IUserDAO;
import com.alejo.persistence.items.IItemDAO;
import com.alejo.service.items.IItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ItemServiceImpl implements IItemService {

    @Autowired
    private IItemDAO itemDAO;

    @Autowired
    private IUserDAO userDAO;

    @Override
    public List<Item> findPaginatedItems(int page, int size) {
        return itemDAO.findPaginatedItems(page, size);
    }

    @Override
    public List<Item> searchAndFilter(String query, List<Integer> categoryIds) {
        return itemDAO.searchAndFilter(query, categoryIds);
    }

    @Override
    public List<Item> searchItems(String query) {
        return itemDAO.search(query);
    }

    @Override
    public List<Item> findByCategories(List<Integer> categoryIds) {
        return itemDAO.findByCategories(categoryIds);
    }

    @Override
    public Optional<Item> findById(Integer id) {
        return itemDAO.findById(id);
    }

    @Override
    public List<Item> findByUser_id(int id) {
        return itemDAO.findByUser_Id(id);
    }

    @Override
    public Item save(ItemDTO item) {

        User existingUser = userDAO.findById(item.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        System.out.println("___________________________________________________ SUSPENDED: " + existingUser.getSuspended());
        if (existingUser.getSuspended()) {
            throw new RuntimeException("Cannot add item: user is suspended");
        }

        Item returnedItem = Item.builder()
                .name(item.getName())
                .description(item.getDescription())
                .image(item.getImage())
                .price(item.getPrice())
                .stock(item.getStock())
                .isNew((item.isNew()))
                .category(Category.builder().id(item.getCategory().getId()).name(item.getCategory().getName()).build())
                .user(existingUser).build();

        return itemDAO.save(returnedItem);
    }

    @Override
    public void update(ItemDTO itemDTO) {
        itemDAO.update(itemDTO);
    }

    @Override
    public void review(ItemDTO item, double rate) {
        itemDAO.review(item, rate);
    }

    @Override
    public List<Item> findItemRequests() {
        return itemDAO.findItemRequests();
    }

    @Override
    public void acceptItem(int id) {
        itemDAO.acceptItemRequest(id);
    }

    @Override
    public void rejectItem(int id) {
        itemDAO.rejectItemRequest(id);
    }

    @Override
    public List<TopClientProductsDTO> getTopClientsByProducts() {
        return itemDAO.findTopClientsByItems();
    }

}
