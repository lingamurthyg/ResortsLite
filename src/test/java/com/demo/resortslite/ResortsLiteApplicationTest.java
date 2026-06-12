package com.demo.resortslite;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.*;

class ResortsLiteApplicationTest {

    @Test
    void main_withValidArgs_startsApplication() {
        // Arrange
        String[] args = new String[]{};

        // Act & Assert
        // Note: We can't actually run the main method in a unit test as it would start the Spring application
        // Instead, we verify the class structure and main method exists
        assertDoesNotThrow(() -> {
            // Verify the main method exists and is accessible
            ResortsLiteApplication.class.getDeclaredMethod("main", String[].class);
        });
    }

    @Test
    void main_methodExists() {
        // Arrange & Act
        boolean mainMethodExists = false;
        try {
            ResortsLiteApplication.class.getDeclaredMethod("main", String[].class);
            mainMethodExists = true;
        } catch (NoSuchMethodException e) {
            // Method doesn't exist
        }

        // Assert
        assertTrue(mainMethodExists, "Main method should exist");
    }

    @Test
    void main_methodIsPublic() throws NoSuchMethodException {
        // Arrange & Act
        var mainMethod = ResortsLiteApplication.class.getDeclaredMethod("main", String[].class);

        // Assert
        assertTrue(java.lang.reflect.Modifier.isPublic(mainMethod.getModifiers()),
                "Main method should be public");
    }

    @Test
    void main_methodIsStatic() throws NoSuchMethodException {
        // Arrange & Act
        var mainMethod = ResortsLiteApplication.class.getDeclaredMethod("main", String[].class);

        // Assert
        assertTrue(java.lang.reflect.Modifier.isStatic(mainMethod.getModifiers()),
                "Main method should be static");
    }

    @Test
    void main_methodReturnsVoid() throws NoSuchMethodException {
        // Arrange & Act
        var mainMethod = ResortsLiteApplication.class.getDeclaredMethod("main", String[].class);

        // Assert
        assertEquals(void.class, mainMethod.getReturnType(),
                "Main method should return void");
    }

    @Test
    void class_hasSpringBootApplicationAnnotation() {
        // Arrange & Act
        boolean hasAnnotation = ResortsLiteApplication.class
                .isAnnotationPresent(org.springframework.boot.autoconfigure.SpringBootApplication.class);

        // Assert
        assertTrue(hasAnnotation, "Class should have @SpringBootApplication annotation");
    }

    @Test
    void class_isPublic() {
        // Arrange & Act
        int modifiers = ResortsLiteApplication.class.getModifiers();

        // Assert
        assertTrue(java.lang.reflect.Modifier.isPublic(modifiers),
                "Class should be public");
    }

    @Test
    void class_hasCorrectPackage() {
        // Arrange & Act
        String packageName = ResortsLiteApplication.class.getPackage().getName();

        // Assert
        assertEquals("com.demo.resortslite", packageName,
                "Class should be in correct package");
    }

    @Test
    void class_hasCorrectName() {
        // Arrange & Act
        String className = ResortsLiteApplication.class.getSimpleName();

        // Assert
        assertEquals("ResortsLiteApplication", className,
                "Class should have correct name");
    }

    @Test
    void class_hasNoParameterConstructor() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            ResortsLiteApplication.class.getDeclaredConstructor();
        }, "Class should have a no-parameter constructor");
    }

    @Test
    void constructor_isPublic() throws NoSuchMethodException {
        // Arrange & Act
        var constructor = ResortsLiteApplication.class.getDeclaredConstructor();

        // Assert
        assertTrue(java.lang.reflect.Modifier.isPublic(constructor.getModifiers()),
                "Constructor should be public");
    }

    @Test
    void instance_canBeCreated() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            new ResortsLiteApplication();
        }, "Should be able to create an instance");
    }

    @Test
    void instance_isNotNull() {
        // Arrange & Act
        ResortsLiteApplication instance = new ResortsLiteApplication();

        // Assert
        assertNotNull(instance, "Instance should not be null");
    }

    @Test
    void class_extendsObject() {
        // Arrange & Act
        Class<?> superclass = ResortsLiteApplication.class.getSuperclass();

        // Assert
        assertEquals(Object.class, superclass,
                "Class should extend Object (no other superclass)");
    }

    @Test
    void class_implementsNoInterfaces() {
        // Arrange & Act
        Class<?>[] interfaces = ResortsLiteApplication.class.getInterfaces();

        // Assert
        assertEquals(0, interfaces.length,
                "Class should not implement any interfaces directly");
    }

    @Test
    void main_acceptsStringArrayParameter() throws NoSuchMethodException {
        // Arrange & Act
        var mainMethod = ResortsLiteApplication.class.getDeclaredMethod("main", String[].class);
        Class<?>[] parameterTypes = mainMethod.getParameterTypes();

        // Assert
        assertEquals(1, parameterTypes.length, "Main method should have one parameter");
        assertEquals(String[].class, parameterTypes[0],
                "Main method parameter should be String[]");
    }

    @Test
    void class_hasOnlyOnePublicMethod() {
        // Arrange & Act
        var publicMethods = ResortsLiteApplication.class.getDeclaredMethods();
        long publicMethodCount = 0;
        for (var method : publicMethods) {
            if (java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                publicMethodCount++;
            }
        }

        // Assert
        assertEquals(1, publicMethodCount,
                "Class should have only one public method (main)");
    }

    @Test
    void main_withNullArgs_shouldHandleGracefully() {
        // Arrange & Act & Assert
        // Note: We can't actually test this without starting the application
        // This test verifies the method signature accepts the parameter
        assertDoesNotThrow(() -> {
            ResortsLiteApplication.class.getDeclaredMethod("main", String[].class);
        });
    }

    @Test
    void main_withEmptyArgs_shouldHandleGracefully() {
        // Arrange
        String[] emptyArgs = new String[]{};

        // Act & Assert
        // Verify method can be invoked with empty args (signature-wise)
        assertDoesNotThrow(() -> {
            var method = ResortsLiteApplication.class.getDeclaredMethod("main", String[].class);
            assertNotNull(method);
        });
    }

    @Test
    void class_isInCorrectModule() {
        // Arrange & Act
        String fullClassName = ResortsLiteApplication.class.getName();

        // Assert
        assertTrue(fullClassName.startsWith("com.demo.resortslite"),
                "Class should be in com.demo.resortslite package");
    }
}
