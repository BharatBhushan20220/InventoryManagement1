package com.example.InventoryManagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int reservedQuantity;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Item_id")
    private Items items;

    private String reservedBy; // Optional: user ID or session ID
    private LocalDateTime reservedAt;
    private LocalDateTime cancelledAt;

    @PrePersist
    public void prePersist() {
        this.reservedAt = LocalDateTime.now();
    }

}
