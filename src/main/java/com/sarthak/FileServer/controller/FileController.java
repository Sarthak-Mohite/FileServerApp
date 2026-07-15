package com.sarthak.FileServer.controller;

import com.sarthak.FileServer.dto.FileMetadataResponse;
import com.sarthak.FileServer.exception.InvalidFileException;
import com.sarthak.FileServer.service.StorageService;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {
  private final StorageService storageService;

  private static final Set<String> ALLOWED_FILE_TYPES =
      Set.of("image/png", "image/jpeg", "application/pdf");

  public FileController(StorageService storageService) {
    this.storageService = storageService;
  }

  @PostMapping
  public ResponseEntity<FileMetadataResponse> uploadFile(@RequestParam("file") MultipartFile file) {
    String fileType = file.getContentType();
    if (!ALLOWED_FILE_TYPES.contains(fileType)) {
      throw new InvalidFileException(
          "Invalid file type: " + fileType + ". Allowed types are: " + ALLOWED_FILE_TYPES);
    }
    FileMetadataResponse response = storageService.uploadFile(file);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{id}/metadata")
  public FileMetadataResponse getMetadata(@PathVariable("id") UUID id) {
    return storageService.getMetadata(id);
  }

  @GetMapping("/{id}")
  public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("id") UUID id) {
    InputStream inputStream = storageService.downloadFile(id);
    InputStreamResource resource = new InputStreamResource(inputStream);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteFile(@PathVariable("id") UUID id) {
    storageService.deleteFile(id);
    return ResponseEntity.noContent().build();
  }
}
