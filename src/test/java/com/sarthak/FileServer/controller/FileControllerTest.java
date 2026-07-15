package com.sarthak.FileServer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sarthak.FileServer.dto.FileMetadataResponse;
import com.sarthak.FileServer.exception.FileNotFoundException;
import com.sarthak.FileServer.service.StorageService;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FileController.class)
public class FileControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private StorageService storageService;

  @Test
  public void uploadFile_withValidType_returns201() throws Exception {
    UUID id = UUID.randomUUID();
    FileMetadataResponse response =
        new FileMetadataResponse(id, "test.png", "image/png", 100L, Instant.now());

    MockMultipartFile file =
        new MockMultipartFile("file", "test.png", "image/png", "fake-bytes".getBytes());

    when(storageService.uploadFile(any())).thenReturn(response);

    mockMvc
        .perform(multipart("/api/v1/files").file(file))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.fileName").value("test.png"));
  }

  @Test
  public void uploadFile_withDisallowedType_returns400() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "resume.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "fake-bytes".getBytes());

    mockMvc
        .perform(multipart("/api/v1/files").file(file))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  public void getMetadata_whenFileExists_returns200() throws Exception {
    UUID id = UUID.randomUUID();
    FileMetadataResponse response =
        new FileMetadataResponse(id, "test.png", "image/png", 100L, Instant.now());

    when(storageService.getMetadata(id)).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/files/{id}/metadata", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fileName").value("test.png"));
  }

  @Test
  public void getMetadata_whenFileMissing_returns404() throws Exception {
    UUID id = UUID.randomUUID();

    when(storageService.getMetadata(id)).thenThrow(new FileNotFoundException(id));

    mockMvc
        .perform(get("/api/v1/files/{id}/metadata", id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  public void downloadFile_whenFileExists_returns200() throws Exception {
    UUID id = UUID.randomUUID();

    when(storageService.downloadFile(id))
        .thenReturn(new ByteArrayInputStream("fake-bytes".getBytes()));

    mockMvc.perform(get("/api/v1/files/{id}", id)).andExpect(status().isOk());
  }

  @Test
  public void deleteFile_whenFileExists_returns204() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(delete("/api/v1/files/{id}", id)).andExpect(status().isNoContent());
  }

  @Test
  public void deleteFile_whenFileMissing_returns404() throws Exception {
    UUID id = UUID.randomUUID();

    org.mockito.Mockito.doThrow(new FileNotFoundException(id)).when(storageService).deleteFile(id);

    mockMvc.perform(delete("/api/v1/files/{id}", id)).andExpect(status().isNotFound());
  }
}
