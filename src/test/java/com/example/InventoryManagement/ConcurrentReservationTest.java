package com.example.InventoryManagement;

import org.springframework.boot.test.context.SpringBootTest;
import com.example.InventoryManagement.entity.Items;
import com.example.InventoryManagement.repository.ItemRepository;
import com.example.InventoryManagement.service.ServiceInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ConcurrentReservationTest {
    @Autowired
    private ServiceInterface inventoryService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Long itemId;

    @BeforeEach
    void setup() {
        itemRepository.deleteAll();
        Items item = Items.builder()
                .name("iPhone 15")
                .sku("IPH15")
                .quantity(10) // only 10 available
                .reservedQuantity(0)
                .price(100000.0)
                .build();

        Items saved = itemRepository.save(item);
        itemId = saved.getId();

        redisTemplate.opsForValue().set("stock:" + itemId, saved.getQuantity());
    }

    @Test
    void test100ConcurrentReservations() throws InterruptedException {
        int totalThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(totalThreads);

        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < totalThreads; i++) {
            int userNum = i;
            Future<Boolean> future = executor.submit(() -> {
                try {
                    inventoryService.reserveItem(itemId, 1, "user-" + userNum);
                    return true;
                } catch (Exception e) {
                    // Could be OutOfStock or other expected errors
                    return false;
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        latch.await(); // Wait for all threads to finish
        executor.shutdown();

        // Count successful reservations
        long successCount = futures.stream().filter(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                return false;
            }
        }).count();

        System.out.println("Successful reservations: " + successCount);

        // ✅ Assert that no more than 10 succeeded
        assertEquals(10, successCount);
    }
}
