package com.alejo.controllers.purchases.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPurchasedDTO {
    private Integer itemId;
    private Integer userId;
    private Integer purchaseId;
    private int quantity;
    private String name;
    private String image;
    private double price;
}
