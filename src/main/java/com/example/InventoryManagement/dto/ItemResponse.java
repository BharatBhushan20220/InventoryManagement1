package com.example.InventoryManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemResponse {

    private Long id;
    private String name;
    private String sku;
    private int quantity;
    private int reservedQuantity;
    private double price;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
