package com.example.InventoryManagement;

import com.example.InventoryManagement.entity.Items;
import com.example.InventoryManagement.entity.Reservation;
import com.example.InventoryManagement.entity.ReservationStatus;
import com.example.InventoryManagement.exceptionHandling.ItemNotFoundException;
import com.example.InventoryManagement.exceptionHandling.ReservationNotFoundException;
import com.example.InventoryManagement.repository.ItemRepository;
import com.example.InventoryManagement.repository.ReservationRepository;
import com.example.InventoryManagement.service.ServiceImplementation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @InjectMocks
    private ServiceImplementation inventoryService;

    public ServiceImplTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testReserveItem_success() {
        Long itemId = 1L;
        int quantity = 2;
        String reservedBy = "Bharat";

        Items item = Items.builder()
                .id(itemId)
                .name("Laptop")
                .quantity(10)
                .reservedQuantity(0)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.decrement("stock:" + itemId, quantity)).thenReturn(8L);
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(itemRepository.save(any(Items.class))).thenAnswer(i -> i.getArguments()[0]);

        Items result = inventoryService.reserveItem(itemId, quantity, reservedBy);

        assertEquals(itemId, result.getId());
        assertEquals(2, result.getReservedQuantity());
    }

    @Test
    void testReserveItem_outOfStock_shouldRollbackRedis() {
        Long itemId = 1L;
        int quantity = 5;

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.decrement("stock:" + itemId, quantity)).thenReturn(-1L);

        assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.reserveItem(itemId, quantity, "TestUser");
        });

        verify(valueOps, times(1)).increment("stock:" + itemId, quantity); // rollback
    }

    @Test
    void testReserveItem_itemNotFound_shouldRollbackRedis() {
        Long itemId = 2L;
        int quantity = 1;

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.decrement("stock:" + itemId, quantity)).thenReturn(9L);
        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class, () -> {
            inventoryService.reserveItem(itemId, quantity, "User");
        });

        verify(valueOps, times(1)).increment("stock:" + itemId, quantity); // rollback
    }

    @Test
    void testCancelReservation_success() {
        Long reservationId = 10L;
        int reservedQty = 3;

        Items item = Items.builder()
                .id(1L)
                .name("MacBook")
                .quantity(10)
                .reservedQuantity(5)
                .build();

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .items(item)
                .reservedQuantity(reservedQty)
                .status(ReservationStatus.RESERVED)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(itemRepository.save(any(Items.class))).thenAnswer(i -> i.getArguments()[0]);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArguments()[0]);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        Items result = inventoryService.cancelReservation(reservationId);

        assertEquals(2, result.getReservedQuantity()); // 5 - 3 = 2
        verify(valueOps, times(1)).increment("stock:" + item.getId(), reservedQty);
    }

    @Test
    void testCancelReservation_reservationNotFound() {
        Long reservationId = 99L;

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () -> {
            inventoryService.cancelReservation(reservationId);
        });
    }

    @Test
    void testCancelReservation_invalidStatus_shouldFail() {
        Long reservationId = 11L;

        Items item = Items.builder()
                .id(1L)
                .name("TV")
                .quantity(20)
                .reservedQuantity(4)
                .build();

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .items(item)
                .reservedQuantity(2)
                .status(ReservationStatus.CANCELLED) // Already cancelled
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalStateException.class, () -> {
            inventoryService.cancelReservation(reservationId);
        });
    }
}
