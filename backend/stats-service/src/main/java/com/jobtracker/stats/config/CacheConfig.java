package com.jobtracker.stats.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobtracker.stats.api.dto.BreakdownResponseDto;
import com.jobtracker.stats.api.dto.StatsSummaryDto;
import com.jobtracker.stats.api.dto.TrendResponseDto;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

import static org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_SUMMARY   = "stats-summary";
    public static final String CACHE_TREND     = "stats-trend";
    public static final String CACHE_BREAKDOWN = "stats-breakdown";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        ObjectMapper om = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Use per-cache typed serializers — avoids @class polymorphism issues
        // with Java records (which are final and skipped by NON_FINAL typing).
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(fromSerializer(new StringRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(cf)
                .withCacheConfiguration(CACHE_SUMMARY,
                        base.serializeValuesWith(fromSerializer(
                                new Jackson2JsonRedisSerializer<>(om, StatsSummaryDto.class))))
                .withCacheConfiguration(CACHE_TREND,
                        base.serializeValuesWith(fromSerializer(
                                new Jackson2JsonRedisSerializer<>(om, TrendResponseDto.class))))
                .withCacheConfiguration(CACHE_BREAKDOWN,
                        base.serializeValuesWith(fromSerializer(
                                new Jackson2JsonRedisSerializer<>(om, BreakdownResponseDto.class))))
                .build();
    }
}
