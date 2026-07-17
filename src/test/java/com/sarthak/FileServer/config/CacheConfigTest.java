package com.sarthak.FileServer.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

public class CacheConfigTest {

  @Test
  public void redisCacheManager_buildsSuccessfully() {
    CacheConfig cacheConfig = new CacheConfig();
    RedisConnectionFactory fakeConnectionFactory = mock(RedisConnectionFactory.class);

    CacheManager result = cacheConfig.redisCacheManager(fakeConnectionFactory);

    assertNotNull(result);
  }
}
