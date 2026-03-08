package com.helmx.tutorial.configuration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityDefaultsPropertiesTest {

    @Test
    void defaultProperties_disableSwaggerAndDebugSecurityLogs() throws IOException {
        Properties properties = loadProperties("application.properties");

        assertEquals("false", properties.getProperty("springdoc.api-docs.enabled"));
        assertEquals("false", properties.getProperty("springdoc.swagger-ui.enabled"));
        assertEquals("false", properties.getProperty("springdoc.swagger-ui.tryItOutEnabled"));
        assertEquals("INFO", properties.getProperty("logging.level.com.helmx.tutorial"));
        assertEquals("WARN", properties.getProperty("logging.level.org.springframework.security"));
        assertEquals("INFO", properties.getProperty("logging.level.org.springframework.web"));
    }

    @Test
    void devProperties_reEnableSwaggerAndVerboseLogs() throws IOException {
        Properties properties = loadProperties("application-dev.properties");

        assertEquals("true", properties.getProperty("springdoc.api-docs.enabled"));
        assertEquals("true", properties.getProperty("springdoc.swagger-ui.enabled"));
        assertEquals("true", properties.getProperty("springdoc.swagger-ui.tryItOutEnabled"));
        assertEquals("DEBUG", properties.getProperty("logging.level.com.helmx.tutorial"));
        assertEquals("DEBUG", properties.getProperty("logging.level.org.springframework.security"));
    }

    @Test
    void prodProperties_requireExplicitCorsOriginAndDisableSwagger() throws IOException {
        Properties properties = loadProperties("application-prod.properties");

        assertEquals("false", properties.getProperty("springdoc.api-docs.enabled"));
        assertEquals("false", properties.getProperty("springdoc.swagger-ui.enabled"));
        assertEquals("false", properties.getProperty("springdoc.swagger-ui.tryItOutEnabled"));
        assertEquals("${APP_CORS_ALLOWED_ORIGIN}", properties.getProperty("app.cors.allowed-origin"));
    }

    private Properties loadProperties(String resourceName) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            properties.load(inputStream);
        }
        return properties;
    }
}
