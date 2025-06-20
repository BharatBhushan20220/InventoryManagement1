package com.example.InventoryManagement;

import com.example.InventoryManagement.dto.ReserveItemRequest;
import com.example.InventoryManagement.entity.Items;
import com.example.InventoryManagement.repository.ItemRepository;
import com.example.InventoryManagement.repository.ReservationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ObjectMapper objectMapper;

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

        redisTemplate.opsForValue().set("stock:" + itemId, saved.getQuantity());
    }

    @Test
    void testReserveItemIntegration_success() throws Exception {
        ReserveItemRequest request = new ReserveItemRequest(2, "bharat@example.com");

        mockMvc.perform(post("/api/items/{id}/reserve", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedQuantity").value(2))
                .andExpect(jsonPath("$.quantity").value(10));

        Object redisValue = redisTemplate.opsForValue().get("stock:" + itemId);
        assert redisValue != null && redisValue.toString().equals("8");
    }
}
