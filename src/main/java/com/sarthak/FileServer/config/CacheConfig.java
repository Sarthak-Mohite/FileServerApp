package com.sarthak.FileServer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
public class CacheConfig {

  @Profile("dev")
  @Bean
  public CacheManager caffeineCacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager("fileMetadata", "fileBytes");

    Cache<Object, Object> fileMetadataCache =
        Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(Duration.ofMinutes(10)).build();

    Cache<Object, Object> fileBytesCache =
        Caffeine.newBuilder()
            .maximumWeight(50L * 1024 * 1024)
            .weigher((Object key, Object value) -> ((byte[]) value).length)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    cacheManager.registerCustomCache("fileMetadata", fileMetadataCache);
    cacheManager.registerCustomCache("fileBytes", fileBytesCache);

    return cacheManager;
  }

  @Profile("prod")
  @Bean
  public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.activateDefaultTyping(
        objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.EVERYTHING);

    GenericJackson2JsonRedisSerializer serializer =
        new GenericJackson2JsonRedisSerializer(objectMapper);

    RedisCacheConfiguration defaultConfig =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer));

    return RedisCacheManager.builder(connectionFactory).cacheDefaults(defaultConfig).build();
  }
}
