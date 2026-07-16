package com.sarthak.FileServer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class FileByteCacheService {

  @Cacheable(value = "fileBytes", key = "#path")
  public byte[] readBytes(Path path) throws IOException {
    return Files.readAllBytes(path);
  }

  @CacheEvict(value = "fileBytes", key = "#path")
  public void evictCache(Path path) {}
}
