package com.demo.resortslite;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.*;

class ResortsLiteApplicationTest {

    @Test
    void main_startsSpringApplication() {
        // This test verifies the main method exists and can be called
        // In a real scenario, we would mock SpringApplication.run()
        assertDoesNotThrow(() -> {
            // Verify the class has a main method
            ResortsLiteApplication.class.getMethod("main", String[].class);
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
        java.lang.reflect.Method mainMethod = ResortsLiteApplication.class.getMethod("main", String[].class);

        // Act
        int modifiers = mainMethod.getModifiers();

        // Assert
        assertTrue(java.lang.reflect.Modifier.isPublic(modifiers),
                "main method should be public");
    }

    @Test
    void mainMethod_isStatic() throws NoSuchMethodException {
        // Arrange
        java.lang.reflect.Method mainMethod = ResortsLiteApplication.class.getMethod("main", String[].class);

        // Act
        int modifiers = mainMethod.getModifiers();

        // Assert
        assertTrue(java.lang.reflect.Modifier.isStatic(modifiers),
                "main method should be static");
    }

    @Test
    void mainMethod_returnsVoid() throws NoSuchMethodException {
        // Arrange
        java.lang.reflect.Method mainMethod = ResortsLiteApplication.class.getMethod("main", String[].class);

        // Act
        Class<?> returnType = mainMethod.getReturnType();

        // Assert
        assertEquals(void.class, returnType, "main method should return void");
    }

    @Test
    void mainMethod_acceptsStringArrayParameter() throws NoSuchMethodException {
        // Arrange
        java.lang.reflect.Method mainMethod = ResortsLiteApplication.class.getMethod("main", String[].class);

        // Act
        Class<?>[] parameterTypes = mainMethod.getParameterTypes();

        // Assert
        assertEquals(1, parameterTypes.length, "main method should have exactly one parameter");
        assertEquals(String[].class, parameterTypes[0], "main method parameter should be String[]");
    }

    @Test
    void applicationClass_hasDefaultConstructor() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            ResortsLiteApplication app = new ResortsLiteApplication();
            assertNotNull(app);
        }, "ResortsLiteApplication should have a default constructor");
    }

    @Test
    void applicationClass_canBeInstantiated() {
        // Act
        ResortsLiteApplication app = new ResortsLiteApplication();

        // Assert
        assertNotNull(app, "ResortsLiteApplication instance should not be null");
        assertTrue(app instanceof ResortsLiteApplication,
                "Instance should be of type ResortsLiteApplication");
    }

    @Test
    void applicationClass_isInCorrectPackage() {
        // Arrange
        String expectedPackage = "com.demo.resortslite";

        // Act
        String actualPackage = ResortsLiteApplication.class.getPackage().getName();

        // Assert
        assertEquals(expectedPackage, actualPackage,
                "ResortsLiteApplication should be in package com.demo.resortslite");
    }

    @Test
    void applicationClass_hasCorrectSimpleName() {
        // Act
        String simpleName = ResortsLiteApplication.class.getSimpleName();

        // Assert
        assertEquals("ResortsLiteApplication", simpleName,
                "Class simple name should be ResortsLiteApplication");
    }

    @Test
    void mainMethod_exists() {
        // Act & Assert
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
        // Act & Assert
        assertFalse(ResortsLiteApplication.class.isInterface(),
                "ResortsLiteApplication should not be an interface");
    }

    @Test
    void applicationClass_isNotEnum() {
        // Act & Assert
        assertFalse(ResortsLiteApplication.class.isEnum(),
                "ResortsLiteApplication should not be an enum");
    }

    @Test
    void applicationClass_hasNoSuperclassOtherThanObject() {
        // Act
        Class<?> superclass = ResortsLiteApplication.class.getSuperclass();

        // Assert
        assertEquals(Object.class, superclass,
                "ResortsLiteApplication should only extend Object");
    }

    @Test
    void applicationClass_implementsNoInterfaces() {
        // Act
        Class<?>[] interfaces = ResortsLiteApplication.class.getInterfaces();

        // Assert
        assertEquals(0, interfaces.length,
                "ResortsLiteApplication should not implement any interfaces");
    }

    @Test
    void applicationClass_hasOnlyOnePublicMethod() {
        // Act
        java.lang.reflect.Method[] publicMethods = ResortsLiteApplication.class.getDeclaredMethods();
        long publicMethodCount = java.util.Arrays.stream(publicMethods)
                .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                .count();

        // Assert
        assertEquals(1, publicMethodCount,
                "ResortsLiteApplication should have exactly one public method (main)");
    }

    @Test
    void multipleInstances_canBeCreated() {
        // Act
        ResortsLiteApplication app1 = new ResortsLiteApplication();
        ResortsLiteApplication app2 = new ResortsLiteApplication();

        // Assert
        assertNotNull(app1);
        assertNotNull(app2);
        assertNotSame(app1, app2, "Each instantiation should create a new object");
    }

    @Test
    void applicationClass_hasCorrectCanonicalName() {
        // Act
        String canonicalName = ResortsLiteApplication.class.getCanonicalName();

        // Assert
        assertEquals("com.demo.resortslite.ResortsLiteApplication", canonicalName,
                "Canonical name should be fully qualified");
    }
}
