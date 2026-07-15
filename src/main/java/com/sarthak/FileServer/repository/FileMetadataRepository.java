package com.sarthak.FileServer.repository;

import com.sarthak.FileServer.entity.FileMetadata;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {}
