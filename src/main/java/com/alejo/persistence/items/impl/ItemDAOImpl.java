package com.alejo.persistence.items.impl;

import com.alejo.controllers.items.dto.ItemDTO;
import com.alejo.controllers.items.dto.TopClientProductsDTO;
import com.alejo.entities.auth.User;
import com.alejo.entities.items.Category;
import com.alejo.entities.items.Item;
import com.alejo.persistence.items.IItemDAO;
import com.alejo.repository.items.ItemRepository;
import com.alejo.repository.items.ItemSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class ItemDAOImpl implements IItemDAO {

    @Autowired
    private ItemRepository itemRepository;

    @Override
    public List<Item> findPaginatedItems(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("Id").ascending());
        Page<Item> items = itemRepository.findAllByAcceptedTrueAndUser_SuspendedFalse(pageable);
        return items.getContent();
    }

    @Override
    public List<Item> search(String query) {
        return itemRepository.search(query);
    }

    @Override
    public List<Item> findByCategories(List<Integer> categories) {
        return itemRepository.findByCategoryIdInAndAcceptedTrue(categories);
    }

    @Override
    public Optional<Item> findById(int id) {
        return itemRepository.findById(id);
    }

    @Override
    public List<Item> findByUser_Id(int id) {
        return itemRepository.findByUser_Id(id);
    }

    @Override
    public Item save(Item item) {
        itemRepository.save(item);
        return itemRepository.findById(item.getId())
                .orElseThrow(() -> new RuntimeException("Item Not Found"));
    }

    @Override
    public void update(ItemDTO itemDTO) {
        Item existingItem = itemRepository.findById(itemDTO.getId())
                .orElseThrow(() -> new RuntimeException("Item not found with ID: " + itemDTO.getId()));

        if (existingItem.getUser().getSuspended()) {
            throw new RuntimeException("Cannot update item: user is suspended");
        }

        existingItem.setName(itemDTO.getName());
        existingItem.setDescription(itemDTO.getDescription());
        existingItem.setImage(itemDTO.getImage());
        existingItem.setPrice(itemDTO.getPrice());
        existingItem.setStock(itemDTO.getStock());
        existingItem.setNew(itemDTO.isNew());

        if (itemDTO.getCategory() != null && itemDTO.getCategory().getId() != null) {
            existingItem.setCategory(
                    Category.builder()
                            .id(itemDTO.getCategory().getId())
                            .name(itemDTO.getCategory().getName())
                            .build()
            );
        }


        itemRepository.save(existingItem);
    }

    @Override
    public void review(ItemDTO itemDTO, double rate) {
        Item existingItem = itemRepository.findById(itemDTO.getId())
                .orElseThrow(() -> new RuntimeException("Item not found with ID: " + itemDTO.getId()));

        existingItem.setRating((existingItem.getRating() * existingItem.getRates() + rate)/(existingItem.getRates() + 1));
        existingItem.setRates(existingItem.getRates() + 1);

        itemRepository.save(existingItem);
    }

    @Override
    public List<Item> findItemRequests() {
        return itemRepository.findAllByAcceptedFalseAndRejectedFalse();
    }

    @Override
    public void acceptItemRequest(int id) {
        Item item = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Item not found with ID: " + id));
        item.setAccepted(true);
        itemRepository.save(item);
    }

    @Override
    public void rejectItemRequest(int id) {
        Item item = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Item not found with ID: " + id));
        item.setRejected(true);
        itemRepository.save(item);
    }

    @Transactional
    @Override
    public List<TopClientProductsDTO> findTopClientsByItems() {
        return itemRepository.findTopUsersByItems()
                .stream()
                .limit(10) // top 10
                .map(obj -> {
                    User user = (User) obj[0];
                    Long total = (Long) obj[1];
                    return new TopClientProductsDTO(user.getId(), user.getName(), total.intValue());
                })
                .toList();
    }

    @Override
    public List<Item> searchAndFilter(String query, List<Integer> categoryIds) {
        return itemRepository.findAll(ItemSpecifications.search(query, categoryIds));
    }

}
