package com.omnixys.cache.health;

import com.omnixys.cache.service.ValkeyService;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

public class ValkeyHealthIndicator implements HealthIndicator {

    private final ValkeyService valkeyService;

    public ValkeyHealthIndicator(ValkeyService valkeyService) {
        this.valkeyService = valkeyService;
    }

    @Override
    public Health health() {
        try {
            com.omnixys.cache.model.CacheHealth ch = valkeyService.health();
            if (ch.healthy()) {
                return Health.up()
                        .withDetail("status", ch.status())
                        .withDetail("latencyMs", ch.latencyMs())
                        .build();
            }
            return Health.down()
                    .withDetail("status", ch.status())
                    .withDetail("error", ch.error())
                    .build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
