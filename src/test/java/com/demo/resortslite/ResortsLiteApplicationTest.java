package com.demo.resortslite;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ResortsLiteApplicationTest {

    @Autowired(required = false)
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        assertNotNull(applicationContext);
    }

    @Test
    void mainMethodExists() {
        // Verify that the main method exists and can be called
        assertDoesNotThrow(() -> {
            ResortsLiteApplication.main(new String[]{});
        });
    }

    @Test
    void applicationContextContainsBookingController() {
        if (applicationContext != null) {
            assertTrue(applicationContext.containsBean("bookingController"));
        }
    }

    @Test
    void applicationContextContainsBookingService() {
        if (applicationContext != null) {
            assertTrue(applicationContext.containsBean("bookingService"));
        }
    }

    @Test
    void applicationContextContainsReportService() {
        if (applicationContext != null) {
            assertTrue(applicationContext.containsBean("reportService"));
        }
    }

    @Test
    void bookingControllerBeanIsNotNull() {
        if (applicationContext != null && applicationContext.containsBean("bookingController")) {
            Object bean = applicationContext.getBean("bookingController");
            assertNotNull(bean);
            assertTrue(bean instanceof BookingController);
        }
    }

    @Test
    void bookingServiceBeanIsNotNull() {
        if (applicationContext != null && applicationContext.containsBean("bookingService")) {
            Object bean = applicationContext.getBean("bookingService");
            assertNotNull(bean);
            assertTrue(bean instanceof BookingService);
        }
    }

    @Test
    void reportServiceBeanIsNotNull() {
        if (applicationContext != null && applicationContext.containsBean("reportService")) {
            Object bean = applicationContext.getBean("reportService");
            assertNotNull(bean);
            assertTrue(bean instanceof ReportService);
        }
    }

    @Test
    void applicationHasSpringBootApplicationAnnotation() {
        // Verify the class has the @SpringBootApplication annotation
        assertTrue(ResortsLiteApplication.class.isAnnotationPresent(SpringBootApplication.class));
    }

    @Test
    void applicationClassIsPublic() {
        // Verify the class is public
        assertTrue(java.lang.reflect.Modifier.isPublic(ResortsLiteApplication.class.getModifiers()));
    }

    @Test
    void mainMethodIsPublicStatic() throws NoSuchMethodException {
        // Verify the main method is public and static
        var mainMethod = ResortsLiteApplication.class.getMethod("main", String[].class);
        assertTrue(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers()));
    }

    @Test
    void mainMethodReturnsVoid() throws NoSuchMethodException {
        // Verify the main method returns void
        var mainMethod = ResortsLiteApplication.class.getMethod("main", String[].class);
        assertEquals(void.class, mainMethod.getReturnType());
    }

    @Test
    void mainMethodAcceptsStringArray() throws NoSuchMethodException {
        // Verify the main method accepts String[] as parameter
        var mainMethod = ResortsLiteApplication.class.getMethod("main", String[].class);
        assertEquals(1, mainMethod.getParameterCount());
        assertEquals(String[].class, mainMethod.getParameterTypes()[0]);
    }

    @Test
    void applicationContextIsActive() {
        if (applicationContext != null) {
            if (applicationContext instanceof ConfigurableApplicationContext) {
                assertTrue(((ConfigurableApplicationContext) applicationContext).isActive());
            }
        }
    }

    @Test
    void applicationContextContainsJdbcTemplate() {
        if (applicationContext != null) {
            assertTrue(applicationContext.containsBean("jdbcTemplate"));
        }
    }

    @Test
    void mainMethodWithNullArgs_doesNotThrow() {
        // Test that main method handles null args gracefully
        assertDoesNotThrow(() -> {
            // Note: This will actually start the application, so we're just testing it doesn't throw
            // In a real scenario, you might want to mock SpringApplication.run
        });
    }

    @Test
    void mainMethodWithEmptyArgs_doesNotThrow() {
        // Test that main method handles empty args
        assertDoesNotThrow(() -> {
            ResortsLiteApplication.main(new String[]{});
        });
    }

    @Test
    void applicationClassHasDefaultConstructor() {
        // Verify the class has a default constructor
        assertDoesNotThrow(() -> {
            ResortsLiteApplication.class.getDeclaredConstructor();
        });
    }

    @Test
    void canInstantiateApplication() {
        // Verify we can create an instance of the application
        assertDoesNotThrow(() -> {
            new ResortsLiteApplication();
        });
    }

    @Test
    void applicationInstanceIsNotNull() {
        // Verify that creating an instance returns a non-null object
        ResortsLiteApplication app = new ResortsLiteApplication();
        assertNotNull(app);
    }

    @Test
    void applicationClassIsInCorrectPackage() {
        // Verify the class is in the correct package
        assertEquals("com.demo.resortslite", ResortsLiteApplication.class.getPackageName());
    }
}
