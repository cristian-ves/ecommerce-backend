package com.alejo.controllers.items;

import com.alejo.controllers.auth.dto.AuthResponseDTO;
import com.alejo.controllers.auth.dto.RegisterRequestDTO;
import com.alejo.controllers.auth.dto.UserDTO;
import com.alejo.controllers.items.dto.*;
import com.alejo.entities.auth.User;
import com.alejo.entities.items.Item;
import com.alejo.security.JwtUtils;
import com.alejo.service.items.IItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/items")
public class ItemController {

    @Autowired
    private IItemService itemService;

    @Autowired
    private JwtUtils jwtUtils;


    @GetMapping
    public List<ItemDTO> getPaginatedItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return itemService.findPaginatedItems(page, size)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/filter")
    public List<ItemDTO> filterItemsByCategory(@RequestParam List<Integer> categories) {
        List<Item> items = itemService.findByCategories(categories);
        return items.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/user/{userId}")
    public List<ItemDTO> findItemsByUser_Id(@PathVariable int userId) {
        List<Item> items = itemService.findByUser_id(userId);
        return items.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @PostMapping("/add")
    public ResponseEntity<?> addItem(@RequestBody ItemDTO itemDTO) {
        if (itemDTO.getName() == null || itemDTO.getName().trim().isEmpty() ||
                itemDTO.getDescription() == null || itemDTO.getDescription().trim().isEmpty() ||
                itemDTO.getImage() == null || itemDTO.getImage().trim().isEmpty() ||
                itemDTO.getPrice() <= 0 ||
                itemDTO.getStock() < 1 ||
                itemDTO.getCategory() == null || itemDTO.getCategory().getId() == null) {

            return ResponseEntity.badRequest().body("All fields are mandatory and must be valid");
        }

        try {
            Item item = itemService.save(itemDTO);
            ItemDTO dto = mapToDTO(item);

            return ResponseEntity.status(201).body(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateItem(@RequestBody ItemDTO itemDTO) {
        try {
            itemService.update(itemDTO);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/review")
    public ResponseEntity<?> reviewItem(@RequestParam("rate") double rate, @RequestBody ItemDTO itemDTO) {
        try {
            itemService.review(itemDTO, rate);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/requests")
    public List<ItemRequestDTO> findItemRequests() {
            List<Item> items = itemService.findItemRequests();
            return items.stream()
                    .map(this::mapToRequestDTO)
                    .collect(Collectors.toList());
    }

    @PostMapping("/accept/{id}")
    public ResponseEntity<?> acceptItem(@PathVariable int id) {
        itemService.acceptItem(id);
        return ResponseEntity.status(203).body("Item accepted");
    }

    @PostMapping("/reject/{id}")
    public ResponseEntity<?> rejectItem(@PathVariable int id) {
        itemService.rejectItem(id);
        return ResponseEntity.status(203).body("Item rejected");
    }

    @GetMapping("/top-users-items")
    public ResponseEntity<List<TopClientProductsDTO>> getTopClientsByItems() {
        List<TopClientProductsDTO> report = itemService.getTopClientsByProducts();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/search")
    public List<ItemDTO> searchItems(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "categories", required = false) List<Integer> categories
    ) {
        return itemService.searchAndFilter(query, categories)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private ItemRequestDTO mapToRequestDTO(Item item) {
        return ItemRequestDTO.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .image(item.getImage())
                .price(item.getPrice())
                .stock(item.getStock())
                .isNew(item.isNew())
                .accepted(item.isAccepted())
                .rejected(item.isRejected())
                .user(UserRequestDTO.builder()
                        .name(item.getUser().getName())
                        .email(item.getUser().getEmail())
                        .suspended(item.getUser().getSuspended())
                        .build())
                .build();
    }

    private ItemDTO mapToDTO(Item item) {
        return ItemDTO.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .image(item.getImage())
                .price(item.getPrice())
                .isNew(item.isNew())
                .accepted(item.isAccepted())
                .rating(item.getRating())
                .rates(item.getRates())
                .category(CategoryDTO.builder()
                        .id(item.getCategory().getId())
                        .name(item.getCategory().getName())
                        .build())
                .user(UserDTO.builder()
                        .id(item.getUser().getId())
                        .role(item.getUser().getRole())
                        .name(item.getUser().getName())
                        .email(item.getUser().getEmail())
                        .suspended(item.getUser().getSuspended())
                        .build())
                .stock(item.getStock())
                .build();
    }
}

