package com.example.InventoryManagement;

import com.example.InventoryManagement.controller.Controller;
import com.example.InventoryManagement.dto.ReserveItemRequest;
import com.example.InventoryManagement.entity.Items;
import com.example.InventoryManagement.repository.ItemRepository;
import com.example.InventoryManagement.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebMvcTest(Controller.class)
@AutoConfigureMockMvc
public class ControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Long itemId;

    @BeforeEach
    void setup() {
        reservationRepository.deleteAll();
        itemRepository.deleteAll();

        Items item = Items.builder()
                .name("MacBook Pro")
                .sku("MBP2024")
                .quantity(10)
                .reservedQuantity(0)
                .price(200000.0)
                .build();

        Items saved = itemRepository.save(item);
        itemId = saved.getId();

        // preload Redis
        redisTemplate.opsForValue().set("stock:" + itemId, saved.getQuantity());
    }

    @Test
    void testReserveItemIntegration_success() {
        ReserveItemRequest request = new ReserveItemRequest(2, "bharat@example.com");

        webTestClient.post()
                .uri("/items/{id}/reserve", itemId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.reservedQuantity").isEqualTo(2)
                .jsonPath("$.quantity").isEqualTo(10);

        // Redis check (stock should be 8 now)
        Object redisValue = redisTemplate.opsForValue().get("stock:" + itemId);
        assert redisValue != null && redisValue.toString().equals("8");
    }
}