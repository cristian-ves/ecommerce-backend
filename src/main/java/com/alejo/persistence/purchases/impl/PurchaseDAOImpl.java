package com.alejo.persistence.purchases.impl;

import com.alejo.controllers.cart.dto.CartItemDTO;
import com.alejo.controllers.purchases.dto.*;
import com.alejo.entities.auth.User;
import com.alejo.entities.cart.CartItem;
import com.alejo.entities.items.Item;
import com.alejo.entities.purchases.ItemPurchased;
import com.alejo.entities.purchases.Purchase;
import com.alejo.entities.purchases.ItemPurchased.ItemPurchasedId;
import com.alejo.entities.purchases.Card;
import com.alejo.persistence.cart.impl.CartItemDAOImpl;
import com.alejo.persistence.purchases.IPurchaseDAO;
import com.alejo.repository.auth.UserRepository;
import com.alejo.repository.items.ItemRepository;
import com.alejo.repository.purchases.CardRepository;
import com.alejo.repository.purchases.ItemPurchasedRepository;
import com.alejo.repository.purchases.PurchaseRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PurchaseDAOImpl implements IPurchaseDAO {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private ItemPurchasedRepository itemPurchasedRepository;
    @Autowired
    private CartItemDAOImpl cartItemDAOImpl;

    @Transactional
    @Override
    public List<TopSellerDTO> findTopEarningUsers(LocalDateTime start, LocalDateTime end) {
        return itemPurchasedRepository.findTopSellersByEarnings(start, end)
                .stream()
                .map(obj -> {
                    Integer userId = (Integer) obj[0];
                    String userName = (String) obj[1];
                    Double totalEarnings = (Double) obj[2];
                    return new TopSellerDTO(userId, userName, totalEarnings);
                })
                .toList();
    }

    @Transactional
    @Override
    public List<TopSellerItemsDTO> findTopSellersByItemsSold(LocalDateTime start, LocalDateTime end) {
        return itemPurchasedRepository.findTopSellersByItemsSold(start, end)
                .stream()
                .map(obj -> {
                    User user = (User) obj[0];
                    Long totalSold = (Long) obj[1];
                    return new TopSellerItemsDTO(user.getId(), user.getName(), totalSold.intValue());
                })
                .limit(5)
                .toList();
    }



    @Transactional
    @Override
    public PurchaseDTO createPurchase(Integer userId, Integer cardId, List<CartItemDTO> items) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        double total = items.stream()
                .mapToDouble(ci -> {
                    Item item = itemRepository.findById(ci.getItemId())
                            .orElseThrow(() -> new IllegalArgumentException("Item not found: " + ci.getItemId()));
                    return item.getPrice() * ci.getQuantity();
                })
                .sum();

        Purchase purchase = Purchase.builder()
                .user(user)
                .card(card)
                .delivered(false)
                .total(total)
                .build();

        purchase = purchaseRepository.save(purchase);

        List<ItemPurchasedDTO> purchasedItems = new ArrayList<>();

        for (CartItemDTO ci : items) {
            Item item = itemRepository.findById(ci.getItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Item not found: " + ci.getItemId()));

            ItemPurchased ip = ItemPurchased.builder()
                    .id(new ItemPurchasedId(userId, item.getId(), purchase.getId()))
                    .user(user)
                    .item(item)
                    .purchase(purchase)
                    .quantity(ci.getQuantity())
                    .build();

            itemPurchasedRepository.save(ip);

            if (item.getStock() < ci.getQuantity()) {
                throw new IllegalArgumentException("Not enough stock for item: " + item.getName());
            }
            item.setStock(item.getStock() - ci.getQuantity());
            itemRepository.save(item);

            CartItem.CartItemId cartItemId = new CartItem.CartItemId(userId, item.getId());
            cartItemDAOImpl.findById(cartItemId).ifPresent(cartItemDAOImpl::delete);

            purchasedItems.add(ItemPurchasedDTO.builder()
                    .itemId(item.getId())
                    .userId(userId)
                    .purchaseId(purchase.getId())
                    .quantity(ci.getQuantity())
                    .name(item.getName())
                    .image(item.getImage())
                    .price(item.getPrice())
                    .build()
            );
        }

        return PurchaseDTO.builder()
                .purchaseId(purchase.getId())
                .userId(userId)
                .cardId(cardId)
                .delivered(false)
                .createdAt(purchase.getCreatedAt())
                .deliveryDate(purchase.getDeliveryDate())
                .items(purchasedItems)
                .total(total)
                .build();
    }


    @Override
    public List<PurchaseDTO> findByUserId(Integer userId) {
        return purchaseRepository.findByUser_Id(userId).stream().map(p -> {
            List<ItemPurchasedDTO> items = itemPurchasedRepository.findByPurchase(p)
                    .stream()
                    .map(ip -> ItemPurchasedDTO.builder()
                            .itemId(ip.getItem().getId())
                            .userId(userId)
                            .purchaseId(p.getId())
                            .quantity(ip.getQuantity())
                            .name(ip.getItem().getName())
                            .image(ip.getItem().getImage())
                            .price(ip.getItem().getPrice())
                            .build()
                    )
                    .collect(Collectors.toList());

            return PurchaseDTO.builder()
                    .purchaseId(p.getId())
                    .userId(userId)
                    .cardId(p.getCard().getId())
                    .delivered(p.getDelivered())
                    .createdAt(p.getCreatedAt())
                    .deliveryDate(p.getDeliveryDate())
                    .total(p.getTotal())
                    .items(items)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<PurchaseDTO> findAll() {
        return purchaseRepository.findAll().stream().map(p -> {
            List<ItemPurchasedDTO> items = itemPurchasedRepository.findByPurchase(p)
                    .stream()
                    .map(ip -> ItemPurchasedDTO.builder()
                            .itemId(ip.getItem().getId())
                            .userId(ip.getUser().getId())
                            .purchaseId(p.getId())
                            .quantity(ip.getQuantity())
                            .name(ip.getItem().getName())
                            .image(ip.getItem().getImage())
                            .price(ip.getItem().getPrice())
                            .build()
                    )
                    .collect(Collectors.toList());

            return PurchaseDTO.builder()
                    .purchaseId(p.getId())
                    .userId(p.getUser().getId())
                    .cardId(p.getCard().getId())
                    .delivered(p.getDelivered())
                    .createdAt(p.getCreatedAt())
                    .deliveryDate(p.getDeliveryDate())
                    .total(p.getTotal())
                    .items(items)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsDelivered(Integer purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found with ID: " + purchaseId));

        purchase.setDelivered(true);
        purchase.setDeliveryDate(java.time.LocalDateTime.now());

        purchaseRepository.save(purchase);
    }

    @Override
    @Transactional
    public Optional<PurchaseDTO> updateDeliveryDate(Integer purchaseId, LocalDateTime newDate) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found"));

        if (newDate.isBefore(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0))) {
            throw new IllegalArgumentException("Delivery date must be today or later");
        }

        purchase.setDeliveryDate(newDate);
        purchaseRepository.save(purchase);

        List<ItemPurchasedDTO> items = itemPurchasedRepository.findByPurchase(purchase)
                .stream()
                .map(ip -> ItemPurchasedDTO.builder()
                        .itemId(ip.getItem().getId())
                        .userId(ip.getUser().getId())
                        .purchaseId(purchase.getId())
                        .quantity(ip.getQuantity())
                        .name(ip.getItem().getName())
                        .image(ip.getItem().getImage())
                        .price(ip.getItem().getPrice())
                        .build()
                )
                .toList();

        PurchaseDTO updated = PurchaseDTO.builder()
                .purchaseId(purchase.getId())
                .userId(purchase.getUser().getId())
                .cardId(purchase.getCard().getId())
                .delivered(purchase.getDelivered())
                .deliveryDate(purchase.getDeliveryDate())
                .createdAt(purchase.getCreatedAt())
                .total(purchase.getTotal())
                .items(items)
                .build();

        return Optional.of(updated);
    }

    @Transactional
    @Override
    public List<TopClientOrdersDTO> findTopClientsByOrders(LocalDateTime start, LocalDateTime end) {
        return purchaseRepository.findTopClientsByOrders(start, end)
                .stream()
                .map(obj -> {
                    User user = (User) obj[0];
                    Long orders = (Long) obj[1];
                    return new TopClientOrdersDTO(user.getId(), user.getName(), orders.intValue());
                })
                .toList();
    }


}
