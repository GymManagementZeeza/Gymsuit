package com.zeezaglobal.gymmanagement.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis-backed Spring Cache for GymSuit's read-heavy data.
 *
 * <p>Cached today:
 * <ul>
 *   <li>{@value #GYMS} — gym profiles, the single hottest read (hit on nearly every request
 *       path via {@code GymService}); 30 min TTL, evicted on any gym write</li>
 *   <li>{@value #MEMBERSHIP_PLANS} — plans per gym, read on every subscription/checkout flow;
 *       15 min TTL, evicted precisely per gym on plan writes</li>
 *   <li>{@value #ACTIVITIES} — the 30 most recent activity-feed entries per gym, read on every
 *       dashboard load; 60 s TTL plus explicit eviction when the activity consumer records</li>
 * </ul>
 * Values are JSON (readable in redis-cli, language-agnostic for future consumers) under the
 * {@code gymsuit:<cache>:} key prefix. A Redis outage degrades to the database instead of
 * failing requests — see {@link #errorHandler()}.
 */
@Configuration
@EnableCaching
@Slf4j
public class RedisConfig implements CachingConfigurer {

    public static final String GYMS = "gyms";
    public static final String MEMBERSHIP_PLANS = "membershipPlans";
    public static final String ACTIVITIES = "activities";

    @Value("${app.cache.gyms-ttl:PT30M}")
    private Duration gymsTtl;

    @Value("${app.cache.membership-plans-ttl:PT15M}")
    private Duration membershipPlansTtl;

    @Value("${app.cache.activities-ttl:PT60S}")
    private Duration activitiesTtl;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "gymsuit:" + cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> perCache = Map.of(
                GYMS, base.entryTtl(gymsTtl),
                MEMBERSHIP_PLANS, base.entryTtl(membershipPlansTtl),
                ACTIVITIES, base.entryTtl(activitiesTtl));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(perCache)
                .transactionAware()
                .build();
    }

    /**
     * A Redis blip must not 500 the API: log it and fall through to the database.
     * Stale-while-revalidate is handled by the short TTLs above.
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            private void warn(String op, Object cache, Object key, RuntimeException e) {
                log.warn("Redis cache {} failed on cache '{}' key '{}'; falling back to database: {}",
                        op, cache, key, e.getMessage());
            }

            @Override
            public void handleCacheGetError(RuntimeException e, org.springframework.cache.Cache cache, Object key) {
                warn("GET", cache.getName(), key, e);
            }

            @Override
            public void handleCachePutError(RuntimeException e, org.springframework.cache.Cache cache, Object key,
                    Object value) {
                warn("PUT", cache.getName(), key, e);
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, org.springframework.cache.Cache cache, Object key) {
                warn("EVICT", cache.getName(), key, e);
            }

            @Override
            public void handleCacheClearError(RuntimeException e, org.springframework.cache.Cache cache) {
                warn("CLEAR", cache.getName(), "-", e);
            }
        };
    }
}
