package com.portshipping.controller;

import com.portshipping.entity.Document;
import com.portshipping.service.DocumentService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public Page<Document> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String docType,
            @RequestParam(required = false) Long shipmentId,
            @RequestParam(required = false) Long customsClearanceId) {
        return documentService.searchDocuments(search, docType, shipmentId, customsClearanceId, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getById(id));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Document> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "shipmentId", required = false) Long shipmentId,
            @RequestParam(value = "customsClearanceId", required = false) Long customsClearanceId,
            Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        Document doc = documentService.uploadDocument(file, documentType, shipmentId, customsClearanceId, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(doc);
    }

    public static class CreateMetadataRequest {
        private String fileName;
        private String documentType;
        private Long shipmentId;
        private Long customsClearanceId;
        private String mimeType;
        private Long fileSizeBytes;
        private String storageKey;

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }

        public Long getShipmentId() { return shipmentId; }
        public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }

        public Long getCustomsClearanceId() { return customsClearanceId; }
        public void setCustomsClearanceId(Long customsClearanceId) { this.customsClearanceId = customsClearanceId; }

        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }

        public Long getFileSizeBytes() { return fileSizeBytes; }
        public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

        public String getStorageKey() { return storageKey; }
        public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    }

    @PostMapping
    public ResponseEntity<Document> registerMetadata(@RequestBody CreateMetadataRequest req, Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        Document doc = documentService.registerMetadataOnly(
                req.getFileName(),
                req.getDocumentType(),
                req.getShipmentId(),
                req.getCustomsClearanceId(),
                req.getMimeType(),
                req.getFileSizeBytes(),
                req.getStorageKey(),
                username
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(doc);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        Document doc = documentService.getById(id);
        Resource resource = documentService.loadDocumentResource(id, username);

        String contentType = doc.getMimeType() != null ? doc.getMimeType() : "application/octet-stream";
        String cleanFileName = doc.getFileName() != null ? doc.getFileName().replaceAll("[^a-zA-Z0-9._-]", "_") : "document.bin";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + cleanFileName + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        documentService.deleteDocument(id, username);
        return ResponseEntity.noContent().build();
    }
}
