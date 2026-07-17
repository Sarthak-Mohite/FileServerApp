package com.sarthak.FileServer.service;

import com.sarthak.FileServer.dto.FileMetadataResponse;
import com.sarthak.FileServer.entity.FileMetadata;
import com.sarthak.FileServer.exception.FileNotFoundException;
import com.sarthak.FileServer.exception.InvalidFileException;
import com.sarthak.FileServer.repository.FileMetadataRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileSystemStorageService implements StorageService {
  private final Path storageLocation = Path.of(System.getProperty("user.dir"), "files");

  public LocalFileSystemStorageService(
      FileMetadataRepository fileMetadataRepository, FileByteCacheService fileByteCacheService) {
    this.fileMetadataRepository = fileMetadataRepository;
    this.fileByteCacheService = fileByteCacheService;
  }

  @PostConstruct
  public void init() {
    try {
      Files.createDirectories(storageLocation);
    } catch (IOException e) {
      throw new RuntimeException("Could not initialize storage location", e);
    }
  }

  private final FileMetadataRepository fileMetadataRepository;
  private final FileByteCacheService fileByteCacheService;

  private static final Logger logger = LoggerFactory.getLogger(LocalFileSystemStorageService.class);

  @Override
  public FileMetadataResponse uploadFile(MultipartFile file) {
    UUID uuid = UUID.randomUUID();

    String originalFileName =
        StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

    if (originalFileName.contains("..")) {
      throw new InvalidFileException("Invalid file name: " + originalFileName);
    }

    Path targetPath = storageLocation.resolve(uuid.toString());

    try {
      file.transferTo(targetPath);

    } catch (IOException e) {
      throw new RuntimeException("Failed to upload file" + originalFileName, e);
    }

    FileMetadata fileMetadata = new FileMetadata();
    fileMetadata.setId(uuid);
    fileMetadata.setFileName(originalFileName);
    fileMetadata.setFileType(file.getContentType());
    fileMetadata.setSize(file.getSize());
    fileMetadata.setUploadDate(Instant.now());

    fileMetadataRepository.save(fileMetadata);
    logger.info("File uploaded: id={}, fileName={}", uuid, originalFileName);

    return new FileMetadataResponse(
        uuid,
        originalFileName,
        file.getContentType(),
        file.getSize(),
        fileMetadata.getUploadDate());
  }

  @Override
  @CacheEvict(value = "fileMetadata", key = "#id")
  public void deleteFile(UUID id) {
    fileMetadataRepository.findById(id).orElseThrow(() -> new FileNotFoundException(id));
    fileMetadataRepository.deleteById(id);
    Path targetPath = storageLocation.resolve(id.toString());
    try {
      Files.deleteIfExists(targetPath);
    } catch (IOException e) {
      throw new RuntimeException("Failed to delete file with id: " + id, e);
    }
    fileByteCacheService.evictCache(targetPath);
    logger.info("File deleted: id={}", id);
  }

  @Override
  @Cacheable(value = "fileMetadata", key = "#id")
  public FileMetadataResponse getMetadata(UUID id) {
    FileMetadata fileMetadata =
        fileMetadataRepository.findById(id).orElseThrow(() -> new FileNotFoundException(id));
    logger.info("Metadata retrieved: id={}, fileName={}", id, fileMetadata.getFileName());

    return new FileMetadataResponse(
        fileMetadata.getId(),
        fileMetadata.getFileName(),
        fileMetadata.getFileType(),
        fileMetadata.getSize(),
        fileMetadata.getUploadDate());
  }

  @Override
  public InputStream downloadFile(UUID id) {

    fileMetadataRepository.findById(id).orElseThrow(() -> new FileNotFoundException(id));
    logger.info("File download initiated: id={}", id);

    Path targetPath = storageLocation.resolve(id.toString());
    if (!Files.exists(targetPath)) {
      throw new FileNotFoundException(id);
    }
    try {
      byte[] bytes = fileByteCacheService.readBytes(targetPath);
      return new java.io.ByteArrayInputStream(bytes);
    } catch (IOException e) {
      throw new RuntimeException("Failed to download file with id: " + id, e);
    }
  }
}
