package com.sarthak.FileServer.exception;

import java.util.UUID;

public class FileNotFoundException extends RuntimeException {

  public FileNotFoundException(String message) {
    super(message);
  }

  public FileNotFoundException(UUID id) {
    super("File not found with id: " + id);
  }
}
