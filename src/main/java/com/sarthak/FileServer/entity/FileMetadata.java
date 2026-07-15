package com.sarthak.FileServer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {

  @Id private UUID id;

  private String fileName;
  private String fileType;
  private long size;
  private Instant uploadDate;
}
