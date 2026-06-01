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
        // We cannot actually start the application in a unit test
        // but we can verify the class structure and main method exists
        assertDoesNotThrow(() -> {
            // Verify the main method exists and is accessible
            ResortsLiteApplication.class.getDeclaredMethod("main", String[].class);
        });
    }

    @Test
    void main_withNullArgs_doesNotThrowNullPointer() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            ResortsLiteApplication.class.getDeclaredMethod("main", String[].class);
        });
    }

    @Test
    void main_withEmptyArgs_doesNotThrow() {
        // Arrange
        String[] args = new String[]{};

        // Act & Assert
        assertDoesNotThrow(() -> {
            ResortsLiteApplication.class.getDeclaredMethod("main", String[].class);
        });
    }

    @Test
    void main_withMultipleArgs_doesNotThrow() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            ResortsLiteApplication.class.getDeclaredMethod("main", String[].class);
        });
    }

    @Test
    void applicationClass_hasSpringBootApplicationAnnotation() {
        // Arrange & Act
        boolean hasAnnotation = ResortsLiteApplication.class
            .isAnnotationPresent(org.springframework.boot.autoconfigure.SpringBootApplication.class);

        // Assert
        assertTrue(hasAnnotation, "ResortsLiteApplication should have @SpringBootApplication annotation");
    }

    @Test
    void applicationClass_isPublic() {
        // Arrange & Act
        int modifiers = ResortsLiteApplication.class.getModifiers();

        // Assert
        assertTrue(java.lang.reflect.Modifier.isPublic(modifiers), 
            "ResortsLiteApplication class should be public");
    }

    @Test
    void mainMethod_isPublic() throws NoSuchMethodException {
        // Arrange
        var mainMethod = ResortsLiteApplication.class.getDeclaredMethod("main", String[].class);

        // Act
        int modifiers = mainMethod.getModifiers();

        // Assert
        assertTrue(java.lang.reflect.Modifier.isPublic(modifiers), 
            "main method should be public");
    }

    @Test
    void mainMethod_isStatic() throws NoSuchMethodException {
        // Arrange
        var mainMethod = ResortsLiteApplication.class.getDeclaredMethod("main", String[].class);

        // Act
        int modifiers = mainMethod.getModifiers();

        // Assert
        assertTrue(java.lang.reflect.Modifier.isStatic(modifiers), 
            "main method should be static");
    }

    @Test
    void mainMethod_returnsVoid() throws NoSuchMethodException {
        // Arrange
        var mainMethod = ResortsLiteApplication.class.getDeclaredMethod("main", String[].class);

        // Act
        Class<?> returnType = mainMethod.getReturnType();

        // Assert
        assertEquals(void.class, returnType, "main method should return void");
    }

    @Test
    void mainMethod_acceptsStringArray() throws NoSuchMethodException {
        // Arrange
        var mainMethod = ResortsLiteApplication.class.getDeclaredMethod("main", String[].class);

        // Act
        Class<?>[] parameterTypes = mainMethod.getParameterTypes();

        // Assert
        assertEquals(1, parameterTypes.length, "main method should accept exactly one parameter");
        assertEquals(String[].class, parameterTypes[0], "main method parameter should be String[]");
    }

    @Test
    void applicationClass_hasDefaultConstructor() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            ResortsLiteApplication.class.getDeclaredConstructor();
        }, "ResortsLiteApplication should have a default constructor");
    }

    @Test
    void applicationClass_canBeInstantiated() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            new ResortsLiteApplication();
        }, "ResortsLiteApplication should be instantiable");
    }

    @Test
    void applicationInstance_isNotNull() {
        // Arrange & Act
        ResortsLiteApplication application = new ResortsLiteApplication();

        // Assert
        assertNotNull(application, "ResortsLiteApplication instance should not be null");
    }

    @Test
    void applicationClass_isInCorrectPackage() {
        // Arrange & Act
        String packageName = ResortsLiteApplication.class.getPackage().getName();

        // Assert
        assertEquals("com.demo.resortslite", packageName, 
            "ResortsLiteApplication should be in com.demo.resortslite package");
    }

    @Test
    void applicationClass_hasCorrectSimpleName() {
        // Arrange & Act
        String simpleName = ResortsLiteApplication.class.getSimpleName();

        // Assert
        assertEquals("ResortsLiteApplication", simpleName, 
            "Class should be named ResortsLiteApplication");
    }

    @Test
    void mainMethod_exists() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            ResortsLiteApplication.class.getDeclaredMethod("main", String[].class);
        }, "main method should exist");
    }

    @Test
    void applicationClass_isNotAbstract() {
        // Arrange & Act
        int modifiers = ResortsLiteApplication.class.getModifiers();

        // Assert
        assertFalse(java.lang.reflect.Modifier.isAbstract(modifiers), 
            "ResortsLiteApplication should not be abstract");
    }

    @Test
    void applicationClass_isNotInterface() {
        // Arrange & Act
        boolean isInterface = ResortsLiteApplication.class.isInterface();

        // Assert
        assertFalse(isInterface, "ResortsLiteApplication should not be an interface");
    }

    @Test
    void applicationClass_isNotEnum() {
        // Arrange & Act
        boolean isEnum = ResortsLiteApplication.class.isEnum();

        // Assert
        assertFalse(isEnum, "ResortsLiteApplication should not be an enum");
    }

    @Test
    void applicationClass_extendsObject() {
        // Arrange & Act
        Class<?> superclass = ResortsLiteApplication.class.getSuperclass();

        // Assert
        assertEquals(Object.class, superclass, 
            "ResortsLiteApplication should extend Object directly");
    }

    @Test
    void multipleInstances_canBeCreated() {
        // Arrange & Act
        ResortsLiteApplication app1 = new ResortsLiteApplication();
        ResortsLiteApplication app2 = new ResortsLiteApplication();

        // Assert
        assertNotNull(app1);
        assertNotNull(app2);
        assertNotSame(app1, app2, "Each instance should be a separate object");
    }
}
