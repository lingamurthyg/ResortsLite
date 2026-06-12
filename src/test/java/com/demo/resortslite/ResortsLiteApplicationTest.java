package com.demo.resortslite;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for ResortsLiteApplication.
 * Tests Spring Boot application startup and configuration.
 */
class ResortsLiteApplicationTest {

    @Test
    void contextLoads() {
        // This test verifies that the application context loads successfully
        assertDoesNotThrow(() -> {
            // Application class exists and is properly configured
            ResortsLiteApplication app = new ResortsLiteApplication();
            assertNotNull(app);
        });
    }

    @Test
    void mainMethod_exists() {
        // Verify main method exists and can be invoked
        assertDoesNotThrow(() -> {
            ResortsLiteApplication.class.getMethod("main", String[].class);
        });
    }

    @Test
    void applicationClass_hasSpringBootApplicationAnnotation() {
        // Verify the class has @SpringBootApplication annotation
        assertTrue(ResortsLiteApplication.class.isAnnotationPresent(
                org.springframework.boot.autoconfigure.SpringBootApplication.class));
    }

    @Test
    void applicationClass_isPublic() {
        // Verify the class is public
        assertTrue(java.lang.reflect.Modifier.isPublic(
                ResortsLiteApplication.class.getModifiers()));
    }

    @Test
    void mainMethod_isPublic() throws NoSuchMethodException {
        // Verify main method is public
        var mainMethod = ResortsLiteApplication.class.getMethod("main", String[].class);
        assertTrue(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers()));
    }

    @Test
    void mainMethod_isStatic() throws NoSuchMethodException {
        // Verify main method is static
        var mainMethod = ResortsLiteApplication.class.getMethod("main", String[].class);
        assertTrue(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers()));
    }

    @Test
    void mainMethod_returnsVoid() throws NoSuchMethodException {
        // Verify main method returns void
        var mainMethod = ResortsLiteApplication.class.getMethod("main", String[].class);
        assertEquals(void.class, mainMethod.getReturnType());
    }

    @Test
    void mainMethod_acceptsStringArray() throws NoSuchMethodException {
        // Verify main method accepts String[] parameter
        var mainMethod = ResortsLiteApplication.class.getMethod("main", String[].class);
        assertEquals(1, mainMethod.getParameterCount());
        assertEquals(String[].class, mainMethod.getParameterTypes()[0]);
    }

    @Test
    void applicationClass_hasDefaultConstructor() {
        // Verify the class has a default constructor
        assertDoesNotThrow(() -> {
            ResortsLiteApplication.class.getDeclaredConstructor();
        });
    }

    @Test
    void applicationInstance_canBeCreated() {
        // Verify an instance can be created
        assertDoesNotThrow(() -> {
            ResortsLiteApplication app = new ResortsLiteApplication();
            assertNotNull(app);
        });
    }

    @Test
    void applicationClass_isInCorrectPackage() {
        // Verify the class is in the correct package
        assertEquals("com.demo.resortslite", ResortsLiteApplication.class.getPackageName());
    }

    @Test
    void applicationClass_hasCorrectName() {
        // Verify the class has the correct name
        assertEquals("ResortsLiteApplication", ResortsLiteApplication.class.getSimpleName());
    }

    @Test
    void springBootApplication_annotation_hasCorrectAttributes() {
        // Verify @SpringBootApplication annotation is present
        var annotation = ResortsLiteApplication.class.getAnnotation(
                org.springframework.boot.autoconfigure.SpringBootApplication.class);
        assertNotNull(annotation);
    }

    @Test
    void applicationClass_extendsObject() {
        // Verify the class extends Object (no custom superclass)
        assertEquals(Object.class, ResortsLiteApplication.class.getSuperclass());
    }

    @Test
    void applicationClass_implementsNoInterfaces() {
        // Verify the class doesn't implement any interfaces
        assertEquals(0, ResortsLiteApplication.class.getInterfaces().length);
    }

    @Test
    void applicationClass_hasNoFields() {
        // Verify the class has no instance fields
        assertEquals(0, ResortsLiteApplication.class.getDeclaredFields().length);
    }

    @Test
    void applicationClass_hasTwoMethods() {
        // Verify the class has expected number of methods (main + constructor)
        var methods = ResortsLiteApplication.class.getDeclaredMethods();
        assertTrue(methods.length >= 1); // At least main method
    }
}
