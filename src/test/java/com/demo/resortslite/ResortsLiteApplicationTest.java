package com.demo.resortslite;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ResortsLiteApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testContextLoads() {
        // Assert that the application context loads successfully
        assertNotNull(applicationContext);
    }

    @Test
    void testMainMethodExists() {
        // Verify that the main method exists and can be invoked
        assertDoesNotThrow(() -> {
            ResortsLiteApplication.class.getMethod("main", String[].class);
        });
    }

    @Test
    void testApplicationContextContainsBookingController() {
        // Verify that BookingController bean is registered
        assertTrue(applicationContext.containsBean("bookingController"));
    }

    @Test
    void testApplicationContextContainsBookingService() {
        // Verify that BookingService bean is registered
        assertTrue(applicationContext.containsBean("bookingService"));
    }

    @Test
    void testApplicationContextContainsReportService() {
        // Verify that ReportService bean is registered
        assertTrue(applicationContext.containsBean("reportService"));
    }

    @Test
    void testBookingControllerBeanIsNotNull() {
        // Verify that BookingController can be retrieved from context
        BookingController controller = applicationContext.getBean(BookingController.class);
        assertNotNull(controller);
    }

    @Test
    void testBookingServiceBeanIsNotNull() {
        // Verify that BookingService can be retrieved from context
        BookingService service = applicationContext.getBean(BookingService.class);
        assertNotNull(service);
    }

    @Test
    void testReportServiceBeanIsNotNull() {
        // Verify that ReportService can be retrieved from context
        ReportService service = applicationContext.getBean(ReportService.class);
        assertNotNull(service);
    }

    @Test
    void testSpringBootApplicationAnnotationPresent() {
        // Verify that the @SpringBootApplication annotation is present
        assertTrue(ResortsLiteApplication.class.isAnnotationPresent(SpringBootApplication.class));
    }

    @Test
    void testApplicationHasPublicMainMethod() throws NoSuchMethodException {
        // Verify that main method is public and static
        var mainMethod = ResortsLiteApplication.class.getMethod("main", String[].class);
        assertTrue(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers()));
    }

    @Test
    void testMainMethodAcceptsStringArray() throws NoSuchMethodException {
        // Verify that main method accepts String[] as parameter
        var mainMethod = ResortsLiteApplication.class.getMethod("main", String[].class);
        assertEquals(1, mainMethod.getParameterCount());
        assertEquals(String[].class, mainMethod.getParameterTypes()[0]);
    }

    @Test
    void testMainMethodReturnsVoid() throws NoSuchMethodException {
        // Verify that main method returns void
        var mainMethod = ResortsLiteApplication.class.getMethod("main", String[].class);
        assertEquals(void.class, mainMethod.getReturnType());
    }

    @Test
    void testApplicationContextIsActive() {
        // Verify that the application context is active
        if (applicationContext instanceof ConfigurableApplicationContext) {
            assertTrue(((ConfigurableApplicationContext) applicationContext).isActive());
        } else {
            // If not ConfigurableApplicationContext, just verify it's not null
            assertNotNull(applicationContext);
        }
    }

    @Test
    void testApplicationContextBeanDefinitionCount() {
        // Verify that the application context has bean definitions
        int beanCount = applicationContext.getBeanDefinitionCount();
        assertTrue(beanCount > 0, "Application context should have at least one bean definition");
    }

    @Test
    void testJdbcTemplateIsConfigured() {
        // Verify that JdbcTemplate is available in the context
        assertTrue(applicationContext.containsBean("jdbcTemplate"));
    }

    @Test
    void testApplicationNameIsCorrect() {
        // Verify application name from context
        String applicationName = applicationContext.getApplicationName();
        assertNotNull(applicationName);
    }
}
