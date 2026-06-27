package com.omnixys.cache.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValkeyKeyDefinitionTest {

    @Test
    void shouldBuildKeyFromParts() {
        var def = new ValkeyKeyDefinition("test");
        assertEquals("test:abc:123", def.key("abc", "123"));
    }

    @Test
    void shouldRejectBlankPrefix() {
        assertThrows(IllegalArgumentException.class, () -> new ValkeyKeyDefinition(""));
    }

    @Test
    void shouldReportTtl() {
        var withTtl = new ValkeyKeyDefinition("tmp", 900);
        assertTrue(withTtl.hasTtl());
        assertEquals(900, withTtl.ttlSeconds());

        var withoutTtl = new ValkeyKeyDefinition("perm");
        assertFalse(withoutTtl.hasTtl());
    }
}
