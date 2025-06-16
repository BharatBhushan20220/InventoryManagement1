package com.example.InventoryManagement;

import com.example.InventoryManagement.entity.Items;
import com.example.InventoryManagement.entity.Reservation;
import com.example.InventoryManagement.entity.ReservationStatus;
import com.example.InventoryManagement.repository.ItemRepository;
import com.example.InventoryManagement.repository.ReservationRepository;
import com.example.InventoryManagement.service.ServiceImplementation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ServiceImplTest {

    @InjectMocks
    private ServiceImplementation serviceImplementation;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Test
    void testCreateItem_success() {
        Items items = Items.builder()
                .name("Laptop")
                .sku("SKU123")
                .quantity(100)
                .price(75000.0)
                .build();

        when(itemRepository.save(any(Items.class))).thenReturn(items);

        Items result = serviceImplementation.createItem(items);

        assertNotNull(result);
        assertEquals("Laptop", result.getName());
        verify(itemRepository).save(items);
    }

    @Test
    void testReserveItem_success() {
        Items items = Items.builder()
                .id(1L)
                .name("Monitor")
                .quantity(100)
                .reservedQuantity(20)
                .build();

        when(itemRepository.findById(1L)).thenReturn(Optional.of(items));
        when(itemRepository.save(any(Items.class))).thenReturn(items);

        Items result = serviceImplementation.reserveItem(1L, 10, "test-user");

        assertEquals(30, result.getReservedQuantity());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void testCancelReservation_invalidStatus_throwsException() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .status(ReservationStatus.CANCELLED)
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalStateException.class, () ->
                serviceImplementation.cancelReservation(1L));
    }
}
