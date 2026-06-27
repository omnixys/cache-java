package com.omnixys.cache.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValkeyRateLimitServiceTest {

    @Mock private StringRedisTemplate redis;

    private ValkeyRateLimitService service;

    @BeforeEach
    void setUp() {
        service = new ValkeyRateLimitService(redis);
    }

    @Test
    void shouldAllowWhenUnderLimit() {
        when(redis.execute(any(DefaultRedisScript.class), any(List.class), any(String.class)))
                .thenReturn(5L);

        assertTrue(service.hit("rate:key:1", 10, 60));
    }

    @Test
    void shouldRejectWhenOverLimit() {
        when(redis.execute(any(DefaultRedisScript.class), any(List.class), any(String.class)))
                .thenReturn(11L);

        assertFalse(service.hit("rate:key:1", 10, 60));
    }

    @Test
    void shouldAllowAtExactLimit() {
        when(redis.execute(any(DefaultRedisScript.class), any(List.class), any(String.class)))
                .thenReturn(10L);

        assertTrue(service.hit("rate:key:1", 10, 60));
    }

    @Test
    void shouldReturnFalseWhenRedisReturnsNull() {
        when(redis.execute(any(DefaultRedisScript.class), any(List.class), any(String.class)))
                .thenReturn(null);

        assertFalse(service.hit("rate:key:1", 10, 60));
    }

    @Test
    void shouldReturnCurrentCount() {
        when(redis.opsForValue()).thenReturn(mock());
        when(redis.opsForValue().get("rate:key:1")).thenReturn("7");

        assertEquals(7, service.current("rate:key:1"));
    }

    @Test
    void shouldReturnZeroWhenNoCurrentValue() {
        when(redis.opsForValue()).thenReturn(mock());
        when(redis.opsForValue().get("rate:key:1")).thenReturn(null);

        assertEquals(0, service.current("rate:key:1"));
    }

    @Test
    void shouldResetKey() {
        service.reset("rate:key:1");
        verify(redis).delete("rate:key:1");
    }
}
