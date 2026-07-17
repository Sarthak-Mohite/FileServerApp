package com.sarthak.FileServer.config;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sarthak.FileServer.entity.FileMetadata;
import com.sarthak.FileServer.repository.FileMetadataRepository;
import com.sarthak.FileServer.service.LocalFileSystemStorageService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("prod")
@Testcontainers
public class RedisCachingIntegrationTest {

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);

  @Autowired private LocalFileSystemStorageService storageService;

  @MockitoBean private FileMetadataRepository fileMetadataRepository;

  @DynamicPropertySource
  static void overrideRedisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }

  @Test
  public void getMetadata_calledTwiceWithSameId_onlyHitsDatabaseOnceViaRedis() {
    UUID fileId = UUID.randomUUID();
    FileMetadata fileMetadata =
        new FileMetadata(fileId, "test.png", "image/png", 100L, Instant.now());

    when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(fileMetadata));

    storageService.getMetadata(fileId);
    storageService.getMetadata(fileId);

    verify(fileMetadataRepository, times(1)).findById(fileId);
  }
}
