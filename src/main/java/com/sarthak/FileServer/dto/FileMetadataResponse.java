package com.sarthak.FileServer.dto;

import java.time.Instant;
import java.util.UUID;

public record FileMetadataResponse(
    UUID id, String fileName, String fileType, long size, Instant uploadDate) {}
