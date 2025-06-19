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

    @PostMapping("/items")
    public ResponseEntity<ItemResponse> createItem(@Valid @RequestBody ItemRequest request) {
        Items item = new Items();
        item.setName(request.getName());
        item.setSku(request.getSku());
        item.setQuantity(request.getQuantity());
        item.setPrice(request.getPrice());
        item.setReservedQuantity(0); // Default

        Items savedItem = serviceInterface.createItem(item);
        return ResponseEntity.ok(toItemResponse(savedItem));
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
    public ResponseEntity<ItemResponse> reserveItem(
            @PathVariable Long itemId,
            @Valid @RequestBody ReserveItemRequest reserveItemRequest) {

        Items item = serviceInterface.reserveItem(
                itemId,
                reserveItemRequest.getQuantity(),
                reserveItemRequest.getReservedBy()
        );

        return ResponseEntity.ok(toItemResponse(item));
    }

    @PostMapping("/reservation/{reservationId}/cancel")
    public ResponseEntity<ItemResponse> cancelReservation(@PathVariable Long reservationId) {
        Items items = serviceInterface.cancelReservation(reservationId);
        return ResponseEntity.ok(toItemResponse(items));
    }

    private ItemResponse toItemResponse(Items item) {
        return ItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .sku(item.getSku())
                .quantity(item.getQuantity())
                .reservedQuantity(item.getReservedQuantity())
                .price(item.getPrice())
                .build();
    }
}
