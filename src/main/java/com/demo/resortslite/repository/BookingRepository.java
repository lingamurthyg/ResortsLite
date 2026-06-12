package com.demo.resortslite.repository;

import com.demo.resortslite.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Booking entity.
 * Provides PostgreSQL-compatible database operations using Spring Data JPA.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    /**
     * Find booking by guest name.
     * 
     * @param guest the guest name
     * @return list of bookings for the guest
     */
    List<Booking> findByGuest(String guest);

    /**
     * Find bookings by room type.
     * 
     * @param room the room type
     * @return list of bookings for the room type
     */
    List<Booking> findByRoom(String room);

    /**
     * Find bookings by check-in date range.
     * PostgreSQL-compatible date range query.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @return list of bookings within the date range
     */
    @Query("SELECT b FROM Booking b WHERE b.checkin BETWEEN :startDate AND :endDate")
    List<Booking> findByCheckinBetween(@Param("startDate") LocalDate startDate, 
                                        @Param("endDate") LocalDate endDate);

    /**
     * Find booking by confirmation code.
     * 
     * @param confirmationCode the confirmation code
     * @return optional booking
     */
    Optional<Booking> findByConfirmationCode(String confirmationCode);

    /**
     * Count bookings by room type.
     * PostgreSQL-compatible count query.
     * 
     * @param room the room type
     * @return count of bookings for the room type
     */
    Long countByRoom(@Param("room") String room);

    /**
     * Find all bookings created after a specific date.
     * PostgreSQL-compatible timestamp query.
     * 
     * @param date the date to filter by
     * @return list of bookings created after the date
     */
    @Query("SELECT b FROM Booking b WHERE b.createdAt > :date ORDER BY b.createdAt DESC")
    List<Booking> findRecentBookings(@Param("date") java.time.LocalDateTime date);

    /**
     * Find bookings by room type and date range.
     * PostgreSQL-compatible complex query.
     * 
     * @param room the room type
     * @param startDate the start date
     * @param endDate the end date
     * @return list of bookings matching criteria
     */
    @Query("SELECT b FROM Booking b WHERE b.room = :room AND b.checkin BETWEEN :startDate AND :endDate")
    List<Booking> findByRoomAndDateRange(@Param("room") String room,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
}
