package com.aidsa.config;

import com.aidsa.modelmanager.AppDataPathResolver;
import com.aidsa.modelmanager.ModelMetadataReader;
import com.aidsa.session.InMemorySessionStore;
import com.aidsa.session.SessionStorePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires domain services from {@code modules/*} into Spring without duplicating logic.
 */
@Configuration
public class CoreModulesConfig {

    @Bean
    public AppDataPathResolver appDataPathResolver() {
        return new AppDataPathResolver();
    }

    @Bean
    public ModelMetadataReader modelMetadataReader(ObjectMapper objectMapper) {
        return new ModelMetadataReader(objectMapper);
    }

    @Bean
    public SessionStorePort sessionStorePort() {
        return new InMemorySessionStore();
    }
}
