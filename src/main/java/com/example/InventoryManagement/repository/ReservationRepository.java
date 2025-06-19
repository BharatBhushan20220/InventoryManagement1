package com.example.InventoryManagement.repository;

import com.example.InventoryManagement.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
