package com.example.InventoryManagement.controller;

import com.example.InventoryManagement.dto.ItemRequest;
import com.example.InventoryManagement.dto.ItemResponse;
import com.example.InventoryManagement.dto.ReserveItemRequest;
import com.example.InventoryManagement.entity.Items;
import com.example.InventoryManagement.exceptionHandling.ResourceNotFoundException;
import com.example.InventoryManagement.service.ServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class Controller {

    private final ServiceInterface serviceInterface;

    //Testing Redis manually
    @GetMapping("/ping")
    @Cacheable("ping")
    public String ping() {
        System.out.println("HIT");
        return "pong";
    }

    //Just Checking controller is working Properly or not
    @GetMapping("/home")
    public ResponseEntity<String> welCome() {
        return ResponseEntity.ok("Welcome to Inventory management Project !!");
    }

    @PostMapping
    public ResponseEntity<ItemResponse> createItems(@Valid @RequestBody ItemRequest itemRequest) {
        Items items = Items.builder()
                .name(itemRequest.getName())
                .sku(itemRequest.getName())
                .quantity(itemRequest.getQuantity())
                .price(itemRequest.getPrice())
                .build();

        Items items1 = serviceInterface.createItem(items);
        return ResponseEntity.ok(toItemResponse(items1));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getItemsById(@PathVariable Long id) {
        return serviceInterface.getItemById(id)
                .map(this::toItemResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Item is found from this given id " + id));
    }

    @GetMapping
    public ResponseEntity<List<ItemResponse>> getAllItems() {
        List<ItemResponse> list = serviceInterface.getAllItems()
                .stream().map(this::toItemResponse).toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{itemId}/reserve")
    public ResponseEntity<ItemResponse> reserveItem(@PathVariable Long itemId, @Valid @RequestBody ReserveItemRequest reserveItemRequest) {
        Items items = serviceInterface.reserveItem(itemId, reserveItemRequest.getQuantity(), reserveItemRequest.getReservedBy());
        return ResponseEntity.ok(toItemResponse(items));
    }

    @PostMapping("/reservation/{reservationId}/cancel")
    public ResponseEntity<ItemResponse> cancelReservation(@PathVariable Long reservationId) {
        Items items = serviceInterface.cancelReservation(reservationId);
        return ResponseEntity.ok(toItemResponse(items));
    }

    private ItemResponse toItemResponse(Items items) {
        return ItemResponse.builder()
                .id(items.getId())
                .name(items.getName())
                .sku(items.getSku())
                .quantity(items.getQuantity())
                .reservedQuantity(items.getReservedQuantity())
                .price(items.getPrice())
                .createdAt(items.getCreatedAt())
                .updatedAt(items.getUpdatedAt())
                .build();
    }
}
