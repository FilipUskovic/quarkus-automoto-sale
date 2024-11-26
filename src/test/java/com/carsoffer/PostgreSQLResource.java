package com.carsoffer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.HashMap;
import java.util.Map;

public class PostgreSQLResource implements QuarkusTestResourceLifecycleManager {

    private PostgreSQLContainer<?> postgres;

    @Override
    public Map<String, String> start() {
        postgres = new PostgreSQLContainer<>("postgres:latest")
                .withDatabaseName("carsoffer_test")
                .withUsername("test")
                .withPassword("test");
        postgres.start();
        Map<String, String> properties = new HashMap<>();
        properties.put("quarkus.datasource.jdbc.url", postgres.getJdbcUrl());
        properties.put("quarkus.datasource.username", postgres.getUsername());
        properties.put("quarkus.datasource.password", postgres.getPassword());
        properties.put("quarkus.flyway.migrate-at-start", "true");


        return properties;
    }

    @Override
    public void stop() {
        if (postgres != null) {
            postgres.stop();
        }
    }
}
