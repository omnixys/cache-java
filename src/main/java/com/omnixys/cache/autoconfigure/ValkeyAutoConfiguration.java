package com.omnixys.cache.autoconfigure;

import com.omnixys.cache.core.CacheSerializer;
import com.omnixys.cache.core.JsonCacheSerializer;
import com.omnixys.cache.health.ValkeyHealthIndicator;
import com.omnixys.cache.service.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@EnableConfigurationProperties(ValkeyProperties.class)
public class ValkeyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheSerializer cacheSerializer() {
        return new JsonCacheSerializer();
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheInvalidationService cacheInvalidationService(
            StringRedisTemplate redis,
            RedisMessageListenerContainer container,
            ValkeyProperties properties) {
        if (properties.getInvalidation().isEnabled()) {
            return new CacheInvalidationService(
                    redis, container,
                    properties.getInvalidation().getChannel(),
                    properties.getKeyPrefix());
        }
        return null;
    }

    @Bean
    @ConditionalOnMissingBean
    public ValkeyService valkeyService(StringRedisTemplate redis, CacheSerializer serializer,
                                       CacheInvalidationService invalidation,
                                       ValkeyProperties properties) {
        return new ValkeyService(redis, serializer, invalidation, properties.getKeyPrefix());
    }

    @Bean
    @ConditionalOnMissingBean
    public ValkeyLockService valkeyLockService(StringRedisTemplate redis) {
        return new ValkeyLockService(redis);
    }

    @Bean
    @ConditionalOnMissingBean
    public ValkeyRateLimitService valkeyRateLimitService(StringRedisTemplate redis) {
        return new ValkeyRateLimitService(redis);
    }

    @Bean
    @ConditionalOnMissingBean
    public ValkeyPubSubService valkeyPubSubService(StringRedisTemplate redis,
                                                   RedisMessageListenerContainer container) {
        return new ValkeyPubSubService(redis, container);
    }

    @Bean
    @ConditionalOnMissingBean
    public ValkeyStreamService valkeyStreamService(StringRedisTemplate redis) {
        return new ValkeyStreamService(redis);
    }

    @Bean
    @ConditionalOnMissingBean
    public DelayedJobRegistry delayedJobRegistry() {
        return new DelayedJobRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public DelayedJobService delayedJobService(StringRedisTemplate redis,
                                               ValkeyStreamService streamService,
                                               ValkeyProperties properties) {
        return new DelayedJobService(redis, streamService, properties.getKeyPrefix());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "omnixys.cache", name = "worker-enabled", havingValue = "true")
    public DelayedJobWorker delayedJobWorker(DelayedJobService jobService,
                                             DelayedJobRegistry registry,
                                             ValkeyProperties properties) {
        return new DelayedJobWorker(jobService, registry, properties.getWorkerPollIntervalMs());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.boot.health.contributor.HealthIndicator")
    public ValkeyHealthIndicator valkeyHealthIndicator(ValkeyService valkeyService) {
        return new ValkeyHealthIndicator(valkeyService);
    }
}
