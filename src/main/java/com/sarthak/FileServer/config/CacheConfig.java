package com.sarthak.FileServer.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
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
}
