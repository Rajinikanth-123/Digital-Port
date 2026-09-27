package com.portshipping.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentStorageService {

    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "doc", "docx", "xls", "xlsx", "csv", "txt"
    );

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv",
            "text/plain",
            "application/octet-stream"
    );

    private final Path storageLocation;

    public DocumentStorageService(@Value("${file.upload-dir:uploads/documents}") String uploadDir) {
        this.storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory at " + this.storageLocation, e);
        }
    }

    public static class StoredFileResult {
        public final String storageKey;
        public final String originalFileName;
        public final String mimeType;
        public final long fileSizeBytes;
        public final String checksumSha256;

        public StoredFileResult(String storageKey, String originalFileName, String mimeType, long fileSizeBytes, String checksumSha256) {
            this.storageKey = storageKey;
            this.originalFileName = originalFileName;
            this.mimeType = mimeType;
            this.fileSizeBytes = fileSizeBytes;
            this.checksumSha256 = checksumSha256;
        }
    }

    public StoredFileResult storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must not be empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "File size (" + (file.getSize() / (1024 * 1024)) + "MB) exceeds maximum limit of 10MB.");
        }

        String rawFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.bin");
        if (rawFilename.contains("..") || rawFilename.contains("/") || rawFilename.contains("\\") || rawFilename.contains("\0")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path/name specified.");
        }

        String extension = getFileExtension(rawFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Disallowed file extension: ." + extension + ". Allowed formats: " + ALLOWED_EXTENSIONS);
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            // Log warning if non-standard MIME but allow if extension is permitted
        }

        String safeStorageName = UUID.randomUUID() + "_" + rawFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String relativeSubDir = LocalDate.now().getYear() + "/" + String.format("%02d", LocalDate.now().getMonthValue());
        Path targetFolder = this.storageLocation.resolve(relativeSubDir).normalize();

        if (!targetFolder.startsWith(this.storageLocation)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path traversal attempt detected.");
        }

        try {
            Files.createDirectories(targetFolder);
            Path targetPath = targetFolder.resolve(safeStorageName).normalize();
            if (!targetPath.startsWith(this.storageLocation)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path traversal attempt detected.");
            }

            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            try (InputStream is = file.getInputStream();
                 DigestInputStream dis = new DigestInputStream(is, sha256)) {
                Files.copy(dis, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String checksumHex = HexFormat.of().formatHex(sha256.digest());
            String relativeStorageKey = relativeSubDir + "/" + safeStorageName;

            return new StoredFileResult(
                    relativeStorageKey,
                    rawFilename,
                    contentType != null ? contentType : "application/octet-stream",
                    file.getSize(),
                    checksumHex
            );
        } catch (NoSuchAlgorithmException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SHA-256 algorithm unavailable", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file: " + e.getMessage(), e);
        }
    }

    public Resource loadFileAsResource(String storageKey) {
        if (storageKey == null || storageKey.isBlank() || storageKey.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage key.");
        }

        try {
            Path filePath = this.storageLocation.resolve(storageKey).normalize();
            if (!filePath.startsWith(this.storageLocation)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path traversal attempt detected.");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file not found on disk.");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not read document file.", e);
        }
    }

    public boolean deleteFile(String storageKey) {
        if (storageKey == null || storageKey.isBlank() || storageKey.contains("..")) return false;
        try {
            Path filePath = this.storageLocation.resolve(storageKey).normalize();
            if (!filePath.startsWith(this.storageLocation)) return false;
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    private String getFileExtension(String filename) {
        int dotIdx = filename.lastIndexOf('.');
        return dotIdx > 0 ? filename.substring(dotIdx + 1) : "";
    }

    public Path getStorageLocation() {
        return storageLocation;
    }
}
