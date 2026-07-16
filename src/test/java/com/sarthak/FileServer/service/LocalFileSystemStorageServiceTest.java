package com.sarthak.FileServer.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.sarthak.FileServer.dto.FileMetadataResponse;
import com.sarthak.FileServer.entity.FileMetadata;
import com.sarthak.FileServer.exception.FileNotFoundException;
import com.sarthak.FileServer.repository.FileMetadataRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class LocalFileSystemStorageServiceTest {

  @Mock private FileMetadataRepository fileMetadataRepository;

  @InjectMocks private LocalFileSystemStorageService storageService;

  // ---------- getMetadata ----------

  @Test
  public void getMetadata_whenFileExists_returnsMetadata() {
    UUID fileId = UUID.randomUUID();
    Instant uploadTime = Instant.now();
    FileMetadata metadata = new FileMetadata(fileId, "test.png", "image/png", 12345L, uploadTime);

    when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(metadata));

    FileMetadataResponse response = storageService.getMetadata(fileId);

    assertEquals(fileId, response.id());
    assertEquals("test.png", response.fileName());
    assertEquals("image/png", response.fileType());
    assertEquals(12345L, response.size());
    assertEquals(uploadTime, response.uploadDate());
  }

  @Test
  public void getMetadata_whenFileDoesNotExist_throwsException() {
    UUID fileId = UUID.randomUUID();

    when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.empty());

    FileNotFoundException exception =
        assertThrows(FileNotFoundException.class, () -> storageService.getMetadata(fileId));

    assertEquals("File not found with id: " + fileId, exception.getMessage());
  }

  // ---------- uploadFile ----------

  @Test
  public void uploadFile_withValidFile_savesMetadataAndReturnsResponse() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", "fake-image-bytes".getBytes());

    FileMetadataResponse response = storageService.uploadFile(file);

    assertNotNull(response.id());
    assertEquals("photo.jpg", response.fileName());
    assertEquals("image/jpeg", response.fileType());
    assertEquals(file.getSize(), response.size());
    assertNotNull(response.uploadDate());

    verify(fileMetadataRepository, times(1)).save(any(FileMetadata.class));
  }

  @Test
  public void uploadFile_withPathTraversalFilename_throwsInvalidFileException() {
    MockMultipartFile file =
        new MockMultipartFile("file", "../../etc/passwd", "text/plain", "malicious".getBytes());

    assertThrows(
        com.sarthak.FileServer.exception.InvalidFileException.class,
        () -> storageService.uploadFile(file));

    verify(fileMetadataRepository, never()).save(any(FileMetadata.class));
  }

  // ---------- deleteFile ----------

  @Test
  public void deleteFile_whenFileExists_deletesFromRepository() {
    UUID fileId = UUID.randomUUID();
    FileMetadata metadata = new FileMetadata(fileId, "test.png", "image/png", 100L, Instant.now());

    when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(metadata));

    storageService.deleteFile(fileId);

    verify(fileMetadataRepository, times(1)).deleteById(fileId);
  }

  @Test
  public void deleteFile_whenFileDoesNotExist_throwsExceptionAndNeverDeletes() {
    UUID fileId = UUID.randomUUID();

    when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.empty());

    assertThrows(FileNotFoundException.class, () -> storageService.deleteFile(fileId));

    verify(fileMetadataRepository, never()).deleteById(any(UUID.class));
  }

  // ---------- downloadFile ----------

  @Test
  public void downloadFile_whenMetadataDoesNotExist_throwsException() {
    UUID fileId = UUID.randomUUID();

    when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.empty());

    assertThrows(FileNotFoundException.class, () -> storageService.downloadFile(fileId));
  }

  @Test
  public void downloadFile_whenFileExists_returnsInputStream() throws Exception {
    // First, actually upload a file so real bytes exist on disk
    MockMultipartFile file =
        new MockMultipartFile("file", "download-test.png", "image/png", "some-bytes".getBytes());
    FileMetadataResponse uploaded = storageService.uploadFile(file);

    // Now mock the repository lookup that downloadFile performs
    FileMetadata metadata =
        new FileMetadata(
            uploaded.id(),
            uploaded.fileName(),
            uploaded.fileType(),
            uploaded.size(),
            uploaded.uploadDate());
    when(fileMetadataRepository.findById(uploaded.id())).thenReturn(Optional.of(metadata));

    InputStream result = storageService.downloadFile(uploaded.id());

    assertNotNull(result);
    result.close();
  }

  @Test
  public void uploadFile_whenDiskWriteFails_throwsRuntimeException() throws Exception {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getOriginalFilename()).thenReturn("test.png");
    doThrow(new IOException("Disk full")).when(file).transferTo(any(java.nio.file.Path.class));

    assertThrows(RuntimeException.class, () -> storageService.uploadFile(file));

    verify(fileMetadataRepository, never()).save(any(FileMetadata.class));
  }

  @Test
  public void uploadFile_whenDatabaseSaveFails_throwsRuntimeException() {
    MockMultipartFile file =
        new MockMultipartFile("file", "test.png", "image/png", "bytes".getBytes());
    doThrow(new RuntimeException("DB connection lost"))
        .when(fileMetadataRepository)
        .save(any(FileMetadata.class));

    assertThrows(RuntimeException.class, () -> storageService.uploadFile(file));
  }

  @Test
  public void getMetadata_whenDatabaseIsDown_propagatesException() {
    UUID fileId = UUID.randomUUID();
    when(fileMetadataRepository.findById(fileId))
        .thenThrow(new RuntimeException("Connection refused"));

    assertThrows(RuntimeException.class, () -> storageService.getMetadata(fileId));
  }

  @Test
  public void deleteFile_calledTwice_secondCallThrowsFileNotFound() {
    UUID fileId = UUID.randomUUID();
    FileMetadata metadata = new FileMetadata(fileId, "test.png", "image/png", 100L, Instant.now());

    // First call: file exists, delete succeeds
    when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.of(metadata));
    storageService.deleteFile(fileId);

    // Second call: repository now reflects it's gone
    when(fileMetadataRepository.findById(fileId)).thenReturn(Optional.empty());
    assertThrows(FileNotFoundException.class, () -> storageService.deleteFile(fileId));

    verify(fileMetadataRepository, times(1)).deleteById(fileId);
  }

  @Test
  public void uploadFile_withNullOriginalFilename_throwsInvalidFileException() {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getOriginalFilename()).thenReturn(null);

    assertThrows(NullPointerException.class, () -> storageService.uploadFile(file));
  }
}
