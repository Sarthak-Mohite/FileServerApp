package com.sarthak.FileServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class FileServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(FileServerApplication.class, args);
  }
}
