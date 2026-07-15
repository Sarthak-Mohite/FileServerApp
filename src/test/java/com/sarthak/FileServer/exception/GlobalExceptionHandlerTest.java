package com.sarthak.FileServer.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  public void handleFileNotFoundException_returns404ProblemDetail() {
    FileNotFoundException ex = new FileNotFoundException("File not found with id: 123");

    ResponseEntity<ProblemDetail> response = handler.handleFileNotFoundException(ex);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("File not found with id: 123", response.getBody().getDetail());
  }

  @Test
  public void handleInvalidFileException_returns400ProblemDetail() {
    InvalidFileException ex = new InvalidFileException("Invalid file type: text/plain");

    ResponseEntity<ProblemDetail> response = handler.handleInvalidFileException(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("Invalid file type: text/plain", response.getBody().getDetail());
  }
}
