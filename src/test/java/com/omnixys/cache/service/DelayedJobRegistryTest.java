package com.omnixys.cache.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DelayedJobRegistryTest {

    @Test
    void shouldRegisterAndExecuteHandler() {
        var registry = new DelayedJobRegistry();
        registry.register("test", (id, payload) -> {
            assertEquals("job-1", id);
            assertEquals("value", payload.get("key"));
        });

        assertTrue(registry.hasHandler("test"));
        registry.execute("test", "job-1", Map.of("key", "value"));
    }

    @Test
    void shouldRejectDuplicateHandlers() {
        var registry = new DelayedJobRegistry();
        registry.register("dup", (id, payload) -> {});
        assertThrows(IllegalArgumentException.class,
                () -> registry.register("dup", (id, payload) -> {}));
    }

    @Test
    void shouldThrowForMissingHandler() {
        var registry = new DelayedJobRegistry();
        assertThrows(IllegalArgumentException.class,
                () -> registry.execute("missing", "id", Map.of()));
    }
}
