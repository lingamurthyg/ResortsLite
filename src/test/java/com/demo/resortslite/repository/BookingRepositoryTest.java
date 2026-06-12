package com.demo.resortslite.repository;

import com.demo.resortslite.entity.Booking;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for BookingRepository interface.
 * Tests repository method signatures and interface structure.
 */
class BookingRepositoryTest {

    @Test
    void bookingRepository_extendsJpaRepository() {
        // Assert
        assertTrue(JpaRepository.class.isAssignableFrom(BookingRepository.class));
    }

    @Test
    void bookingRepository_hasRepositoryAnnotation() {
        // Assert
        assertTrue(BookingRepository.class.isAnnotationPresent(
                org.springframework.stereotype.Repository.class));
    }

    @Test
    void bookingRepository_isInterface() {
        // Assert
        assertTrue(BookingRepository.class.isInterface());
    }

    @Test
    void findByGuest_methodExists() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findByGuest", String.class);

        // Assert
        assertNotNull(method);
        assertEquals(List.class, method.getReturnType());
    }

    @Test
    void findByRoom_methodExists() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findByRoom", String.class);

        // Assert
        assertNotNull(method);
        assertEquals(List.class, method.getReturnType());
    }

    @Test
    void findByCheckinBetween_methodExists() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findByCheckinBetween", 
                LocalDate.class, LocalDate.class);

        // Assert
        assertNotNull(method);
        assertEquals(List.class, method.getReturnType());
    }

    @Test
    void findByConfirmationCode_methodExists() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findByConfirmationCode", String.class);

        // Assert
        assertNotNull(method);
        assertEquals(Optional.class, method.getReturnType());
    }

    @Test
    void countByRoom_methodExists() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("countByRoom", String.class);

        // Assert
        assertNotNull(method);
        assertEquals(Long.class, method.getReturnType());
    }

    @Test
    void findRecentBookings_methodExists() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findRecentBookings", 
                java.time.LocalDateTime.class);

        // Assert
        assertNotNull(method);
        assertEquals(List.class, method.getReturnType());
    }

    @Test
    void findByRoomAndDateRange_methodExists() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findByRoomAndDateRange", 
                String.class, LocalDate.class, LocalDate.class);

        // Assert
        assertNotNull(method);
        assertEquals(List.class, method.getReturnType());
    }

    @Test
    void findByGuest_hasCorrectParameterType() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findByGuest", String.class);

        // Assert
        assertEquals(1, method.getParameterCount());
        assertEquals(String.class, method.getParameterTypes()[0]);
    }

    @Test
    void findByRoom_hasCorrectParameterType() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findByRoom", String.class);

        // Assert
        assertEquals(1, method.getParameterCount());
        assertEquals(String.class, method.getParameterTypes()[0]);
    }

    @Test
    void findByCheckinBetween_hasCorrectParameterTypes() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findByCheckinBetween", 
                LocalDate.class, LocalDate.class);

        // Assert
        assertEquals(2, method.getParameterCount());
        assertEquals(LocalDate.class, method.getParameterTypes()[0]);
        assertEquals(LocalDate.class, method.getParameterTypes()[1]);
    }

    @Test
    void findByConfirmationCode_hasCorrectParameterType() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findByConfirmationCode", String.class);

        // Assert
        assertEquals(1, method.getParameterCount());
        assertEquals(String.class, method.getParameterTypes()[0]);
    }

    @Test
    void countByRoom_hasCorrectParameterType() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("countByRoom", String.class);

        // Assert
        assertEquals(1, method.getParameterCount());
        assertEquals(String.class, method.getParameterTypes()[0]);
    }

    @Test
    void findRecentBookings_hasCorrectParameterType() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findRecentBookings", 
                java.time.LocalDateTime.class);

        // Assert
        assertEquals(1, method.getParameterCount());
        assertEquals(java.time.LocalDateTime.class, method.getParameterTypes()[0]);
    }

    @Test
    void findByRoomAndDateRange_hasCorrectParameterTypes() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findByRoomAndDateRange", 
                String.class, LocalDate.class, LocalDate.class);

        // Assert
        assertEquals(3, method.getParameterCount());
        assertEquals(String.class, method.getParameterTypes()[0]);
        assertEquals(LocalDate.class, method.getParameterTypes()[1]);
        assertEquals(LocalDate.class, method.getParameterTypes()[2]);
    }

    @Test
    void findByCheckinBetween_hasQueryAnnotation() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findByCheckinBetween", 
                LocalDate.class, LocalDate.class);

        // Assert
        assertTrue(method.isAnnotationPresent(
                org.springframework.data.jpa.repository.Query.class));
    }

    @Test
    void findRecentBookings_hasQueryAnnotation() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findRecentBookings", 
                java.time.LocalDateTime.class);

        // Assert
        assertTrue(method.isAnnotationPresent(
                org.springframework.data.jpa.repository.Query.class));
    }

    @Test
    void findByRoomAndDateRange_hasQueryAnnotation() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findByRoomAndDateRange", 
                String.class, LocalDate.class, LocalDate.class);

        // Assert
        assertTrue(method.isAnnotationPresent(
                org.springframework.data.jpa.repository.Query.class));
    }

    @Test
    void bookingRepository_isPublic() {
        // Assert
        assertTrue(java.lang.reflect.Modifier.isPublic(
                BookingRepository.class.getModifiers()));
    }

    @Test
    void bookingRepository_isInCorrectPackage() {
        // Assert
        assertEquals("com.demo.resortslite.repository", 
                BookingRepository.class.getPackageName());
    }

    @Test
    void bookingRepository_hasCorrectName() {
        // Assert
        assertEquals("BookingRepository", BookingRepository.class.getSimpleName());
    }

    @Test
    void bookingRepository_extendsJpaRepositoryWithCorrectTypes() {
        // Assert
        var interfaces = BookingRepository.class.getGenericInterfaces();
        assertTrue(interfaces.length > 0);
        String interfaceString = interfaces[0].toString();
        assertTrue(interfaceString.contains("Booking"));
        assertTrue(interfaceString.contains("String"));
    }

    @Test
    void findByGuest_isPublicMethod() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findByGuest", String.class);

        // Assert
        assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
    }

    @Test
    void findByRoom_isPublicMethod() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findByRoom", String.class);

        // Assert
        assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
    }

    @Test
    void findByConfirmationCode_isPublicMethod() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("findByConfirmationCode", String.class);

        // Assert
        assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
    }

    @Test
    void countByRoom_isPublicMethod() throws NoSuchMethodException {
        // Act
        Method method = BookingRepository.class.getMethod("countByRoom", String.class);

        // Assert
        assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
    }

    @Test
    void bookingRepository_hasExpectedNumberOfMethods() {
        // Act
        Method[] methods = BookingRepository.class.getDeclaredMethods();

        // Assert
        assertTrue(methods.length >= 7); // At least 7 custom methods
    }

    @Test
    void bookingRepository_inheritsStandardJpaRepositoryMethods() {
        // Assert - verify key JPA repository methods are available
        assertDoesNotThrow(() -> {
            BookingRepository.class.getMethod("findById", Object.class);
            BookingRepository.class.getMethod("save", Object.class);
            BookingRepository.class.getMethod("findAll");
            BookingRepository.class.getMethod("deleteById", Object.class);
        });
    }
}
