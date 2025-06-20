package com.example.InventoryManagement.service;

import com.example.InventoryManagement.entity.Items;
import com.example.InventoryManagement.entity.Reservation;
import com.example.InventoryManagement.entity.ReservationStatus;
import com.example.InventoryManagement.exceptionHandling.ItemNotFoundException;
import com.example.InventoryManagement.exceptionHandling.OutOfStockException;
import com.example.InventoryManagement.exceptionHandling.ReservationNotFoundException;
import com.example.InventoryManagement.repository.ItemRepository;
import com.example.InventoryManagement.repository.ReservationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServiceImplementation implements ServiceInterface {

    private final ItemRepository itemRepository;
    private final ReservationRepository reservationRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @CacheEvict(value = "items", key = "#item.id")
    @Override
    public Items createItem(Items item) {
        // Set default reserved quantity if not already set
        if (item.getReservedQuantity() == null) {
            item.setReservedQuantity(0);
        }

        Items saved = itemRepository.save(item);

        // Redis key pattern: stock:{itemId}
        String redisKey = "stock:" + saved.getId();
        redisTemplate.opsForValue().set(redisKey, saved.getQuantity());

        return saved;
    }

    @Cacheable(value = "items", key = "#id")
    @Override
    public Optional<Items> getItemById(Long id) {
        return itemRepository.findById(id);
    }

    @Override
    public List<Items> getAllItems() {
        return itemRepository.findAll();
    }

    @Retryable(
            value = {DeadlockLoserDataAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    @CacheEvict(value = "items", key = "#itemId")
    @Override
    @Transactional
    public Items reserveItem(Long itemId, int quantity, String reservedBy) {
        String redisKey = "stock:" + itemId;

        // 1. Atomically decrement stock in Redis
        Long newStock = redisTemplate.opsForValue().decrement(redisKey, quantity);

        if (newStock == null) {
            throw new RuntimeException("Redis is unavailable or stock key is missing.");
        }

        if (newStock < 0) {
            // 2. Rollback if stock goes below 0
            redisTemplate.opsForValue().increment(redisKey, quantity);
            throw new OutOfStockException(itemId);
        }

        try {
            // 3. Fetch item from DB
            Items items = itemRepository.findById(itemId)
                    .orElseThrow(() -> new ItemNotFoundException(itemId));

            // 4. Create and save reservation
            Reservation reservation = Reservation.builder()
                    .items(items)
                    .reservedQuantity(quantity)
                    .status(ReservationStatus.RESERVED)
                    .reservedBy(reservedBy)
                    .reservedAt(LocalDateTime.now())
                    .build();
            reservationRepository.save(reservation);

            // 5. Update reservedQuantity and save item
            items.setReservedQuantity(items.getReservedQuantity() + quantity);
            return itemRepository.save(items);

        } catch (Exception e) {
            // 6. Rollback Redis if anything fails in DB
            redisTemplate.opsForValue().increment(redisKey, quantity);
            throw new RuntimeException("Reservation failed. Redis stock rolled back.", e);
        }
    }

    @CacheEvict(value = "items", key = "#reservationId")
    @Override
    @Transactional
    public Items cancelReservation(Long reservationId) {
        // 1. Fetch reservation from DB
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        // 2. Check status
        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Only RESERVED reservations can be cancelled.");
        }

        // 3. Get the item
        Items item = reservation.getItems();

        // 4. Update reservation
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        // 5. Update DB item’s reserved quantity
        item.setReservedQuantity(item.getReservedQuantity() - reservation.getReservedQuantity());
        itemRepository.save(item);

        // 6. Increment Redis stock back
        String redisKey = "stock:" + item.getId();
        redisTemplate.opsForValue().increment(redisKey, reservation.getReservedQuantity());

        return item;
    }
}
