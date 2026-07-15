package com.sarthak.FileServer.service;

import com.sarthak.FileServer.dto.FileMetadataResponse;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

  FileMetadataResponse uploadFile(MultipartFile file);

  InputStream downloadFile(UUID id);

  FileMetadataResponse getMetadata(UUID id);

  void deleteFile(UUID id);
}
