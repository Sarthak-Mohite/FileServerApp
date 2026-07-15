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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileSystemStorageService implements StorageService {
  private final Path storageLocation = Path.of(System.getProperty("user.dir"), "files");

  @PostConstruct
  public void init() {
    try {
      Files.createDirectories(storageLocation);
    } catch (IOException e) {
      throw new RuntimeException("Could not initialize storage location", e);
    }
  }

  private final FileMetadataRepository fileMetadataRepository;

  public LocalFileSystemStorageService(FileMetadataRepository fileMetadataRepository) {
    this.fileMetadataRepository = fileMetadataRepository;
  }

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
      ;
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

    return new FileMetadataResponse(
        uuid,
        originalFileName,
        file.getContentType(),
        file.getSize(),
        fileMetadata.getUploadDate());
  }

  @Override
  public void deleteFile(UUID id) {
    fileMetadataRepository.findById(id).orElseThrow(() -> new FileNotFoundException(id));
    fileMetadataRepository.deleteById(id);
    Path targetPath = storageLocation.resolve(id.toString());
    try {
      Files.deleteIfExists(targetPath);
    } catch (IOException e) {
      throw new RuntimeException("Failed to delete file with id: " + id, e);
    }
  }

  @Override
  public FileMetadataResponse getMetadata(UUID id) {
    FileMetadata fileMetadata =
        fileMetadataRepository.findById(id).orElseThrow(() -> new FileNotFoundException(id));

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

    Path targetPath = storageLocation.resolve(id.toString());
    if (!Files.exists(targetPath)) {
      throw new FileNotFoundException(id);
    }
    try {
      return Files.newInputStream(targetPath);
    } catch (IOException e) {
      throw new RuntimeException("Failed to download file with id: " + id, e);
    }
  }
}
