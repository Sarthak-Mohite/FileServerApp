package com.sarthak.FileServer.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sarthak.FileServer.entity.FileMetadata;
import com.sarthak.FileServer.repository.FileMetadataRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class LocalFileSystemStorageServiceCachingTest {

  @Autowired private LocalFileSystemStorageService localFileSystemStorageService;

  @MockitoBean private FileMetadataRepository fileMetadataRepository;
  @MockitoBean private FileByteCacheService fileByteCacheService;

  @Autowired private StorageService storageService;

  @Test
  public void getMetadata_calledTwice_OnlyHitsDbOnce() {
    UUID fileId = UUID.randomUUID();

    FileMetadata fileMetadata =
        new FileMetadata(fileId, "test.png", "image/png", 100L, Instant.now());
    when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(fileMetadata));

    storageService.getMetadata(fileId);
    storageService.getMetadata(fileId);

    verify(fileMetadataRepository, times(1)).findById(fileId);
  }

  @Test
  public void deleting_file_clearsCache() {
    UUID fileId = UUID.randomUUID();

    FileMetadata fileMetadata =
        new FileMetadata(fileId, "test.png", "image/png", 100L, Instant.now());

    when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(fileMetadata));

    storageService.getMetadata(fileId);
    storageService.deleteFile(fileId);
    storageService.getMetadata(fileId);

    verify(fileMetadataRepository, times(3)).findById(fileId);
  }

  @Test
  public void downloadFile_calledTwice_delegatesToCacheServiceEachTime() throws IOException {
    UUID fileId = UUID.randomUUID();
    FileMetadata fileMetadata =
        new FileMetadata(fileId, "test.png", "image/png", 100L, Instant.now());
    when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(fileMetadata));
    when(fileByteCacheService.readBytes(any(Path.class))).thenReturn("test-bytes".getBytes());

    Path testPath = Path.of(System.getProperty("user.dir"), "files", fileId.toString());
    Files.write(testPath, "test-bytes".getBytes());

    storageService.downloadFile(fileId);
    storageService.downloadFile(fileId);
    verify(fileByteCacheService, times(2)).readBytes(any(Path.class));
  }
}
