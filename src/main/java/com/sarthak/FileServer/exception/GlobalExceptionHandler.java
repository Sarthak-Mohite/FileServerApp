package com.sarthak.FileServer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(FileNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleFileNotFoundException(FileNotFoundException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
  }

  @ExceptionHandler(InvalidFileException.class)
  public ResponseEntity<ProblemDetail> handleInvalidFileException(InvalidFileException ex) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
  }
}
