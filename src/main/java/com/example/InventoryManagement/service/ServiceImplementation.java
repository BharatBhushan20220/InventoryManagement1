package com.example.InventoryManagement.service;

import com.example.InventoryManagement.entity.Items;
import com.example.InventoryManagement.entity.Reservation;
import com.example.InventoryManagement.entity.ReservationStatus;
import com.example.InventoryManagement.repository.ItemRepository;
import com.example.InventoryManagement.repository.ReservationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServiceImplementation implements ServiceInterface {

    private final ItemRepository itemRepository;
    private final ReservationRepository reservationRepository;

    @Override
    public Items createItem(Items item) {
        return itemRepository.save(item);
    }

    @Override
    public Optional<Items> getItemById(Long id) {
        return itemRepository.findById(id);
    }

    @Override
    public List<Items> getAllItems() {
        return itemRepository.findAll();
    }

    @Override
    @Transactional
    public Items reserveItem(Long itemId, int quantity, String reservedBy) {
        Items items = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found ! "));

        if (items.getQuantity() - items.getReservedQuantity() < quantity) {
            throw new IllegalArgumentException("Quantity is not available on the demand ");
        }
        items.setReservedQuantity(items.getReservedQuantity() + quantity);

        Reservation reservation = Reservation.builder()
                .items(items)
                .reservedQuantity(quantity)
                .status(ReservationStatus.RESERVED)
                .reservedBy(reservedBy)
                .build();

        reservationRepository.save(reservation);
        return itemRepository.save(items);
    }

    @Override
    @Transactional
    public Items cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation of this Id is not available !! "));

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new IllegalStateException("Only reserve status reservation will be cancelled");
        }
        Items items = reservation.getItems();
        items.setReservedQuantity(items.getReservedQuantity() - reservation.getReservedQuantity());
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        reservationRepository.save(reservation);
        return itemRepository.save(items);
    }
}
