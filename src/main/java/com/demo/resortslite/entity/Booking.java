package com.demo.resortslite.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * Booking entity for PostgreSQL database.
 * Uses JPA annotations for ORM mapping with snake_case naming convention.
 */
@Entity
@Table(name = "bookings", schema = "public")
public class Booking {

    @Id
    @Column(name = "id", nullable = false, length = 50)
    @NotNull(message = "Booking ID cannot be null")
    private String id;

    @Column(name = "guest", nullable = false, length = 255)
    @NotBlank(message = "Guest name cannot be blank")
    private String guest;

    @Column(name = "room", nullable = false, length = 100)
    @NotBlank(message = "Room type cannot be blank")
    private String room;

    @Column(name = "checkin", nullable = false)
    @NotNull(message = "Check-in date cannot be null")
    private LocalDate checkin;

    @Column(name = "checkout", nullable = false)
    @NotNull(message = "Check-out date cannot be null")
    private LocalDate checkout;

    @Column(name = "confirmation_code", length = 100)
    private String confirmationCode;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    // Constructors
    public Booking() {
        this.createdAt = java.time.LocalDateTime.now();
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public Booking(String id, String guest, String room, LocalDate checkin, LocalDate checkout) {
        this.id = id;
        this.guest = guest;
        this.room = room;
        this.checkin = checkin;
        this.checkout = checkout;
        this.createdAt = java.time.LocalDateTime.now();
        this.updatedAt = java.time.LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGuest() {
        return guest;
    }

    public void setGuest(String guest) {
        this.guest = guest;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public LocalDate getCheckin() {
        return checkin;
    }

    public void setCheckin(LocalDate checkin) {
        this.checkin = checkin;
    }

    public LocalDate getCheckout() {
        return checkout;
    }

    public void setCheckout(LocalDate checkout) {
        this.checkout = checkout;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = java.time.LocalDateTime.now();
    }
}
