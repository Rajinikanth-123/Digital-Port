package com.portshipping.service;

import com.portshipping.entity.*;
import com.portshipping.repository.*;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class DocumentService {

    private static final Set<String> VALID_DOC_TYPES = Set.of(
            "BILL_OF_LADING", "CARGO_MANIFEST", "CUSTOMS_DECLARATION",
            "COMMERCIAL_INVOICE", "CERTIFICATE_OF_ORIGIN", "PACKING_LIST", "OTHER"
    );

    private final DocumentRepository documentRepository;
    private final ShipmentRepository shipmentRepository;
    private final CustomsClearanceRepository customsClearanceRepository;
    private final UserRepository userRepository;
    private final DocumentStorageService storageService;
    private final AuditLogRepository auditLogRepository;

    public DocumentService(DocumentRepository documentRepository,
                           ShipmentRepository shipmentRepository,
                           CustomsClearanceRepository customsClearanceRepository,
                           UserRepository userRepository,
                           DocumentStorageService storageService,
                           AuditLogRepository auditLogRepository) {
        this.documentRepository = documentRepository;
        this.shipmentRepository = shipmentRepository;
        this.customsClearanceRepository = customsClearanceRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<Document> searchDocuments(String search, String docType, Long shipmentId, Long customsClearanceId, Pageable pageable) {
        if (shipmentId != null) {
            List<Document> list = documentRepository.findByShipmentId(shipmentId);
            return new org.springframework.data.domain.PageImpl<>(list, pageable, list.size());
        }
        if (customsClearanceId != null) {
            List<Document> list = documentRepository.findByCustomsClearanceId(customsClearanceId);
            return new org.springframework.data.domain.PageImpl<>(list, pageable, list.size());
        }
        if (docType != null && !docType.isBlank() && !"ALL".equalsIgnoreCase(docType)) {
            return documentRepository.findByDocumentTypeIgnoreCase(docType.trim(), pageable);
        }
        if (search != null && !search.isBlank()) {
            return documentRepository.findByFileNameContainingIgnoreCaseOrDocumentTypeContainingIgnoreCase(search.trim(), search.trim(), pageable);
        }
        return documentRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Document getById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document #" + id + " not found"));
    }

    public Document uploadDocument(MultipartFile file, String documentType, Long shipmentId, Long customsClearanceId, String username) {
        if (documentType == null || documentType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document type is required.");
        }

        String typeClean = documentType.trim().toUpperCase();
        if (!VALID_DOC_TYPES.contains(typeClean)) {
            typeClean = "OTHER";
        }

        Shipment shipment = null;
        if (shipmentId != null) {
            shipment = shipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked Shipment #" + shipmentId + " not found."));
        }

        CustomsClearance clearance = null;
        if (customsClearanceId != null) {
            clearance = customsClearanceRepository.findById(customsClearanceId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked Customs Clearance #" + customsClearanceId + " not found."));
            if (shipment == null && clearance.getShipment() != null) {
                shipment = clearance.getShipment();
            }
        }

        // Store physical file
        DocumentStorageService.StoredFileResult result = storageService.storeFile(file);

        User activeUser = findUserByEmail(username);

        Document doc = new Document(
                shipment,
                clearance,
                typeClean,
                result.originalFileName,
                result.storageKey,
                result.mimeType,
                result.fileSizeBytes,
                result.checksumSha256,
                activeUser
        );

        Document saved = documentRepository.save(doc);

        auditLogRepository.save(new AuditLog(activeUser, "UPLOAD_DOCUMENT", "Document", saved.getId(),
                "{\"fileName\":\"" + saved.getFileName() + "\",\"documentType\":\"" + typeClean + "\",\"size\":" + saved.getFileSizeBytes() + "}"));

        return saved;
    }

    public Document registerMetadataOnly(String fileName, String documentType, Long shipmentId, Long customsClearanceId, String mimeType, Long sizeBytes, String storageKey, String username) {
        if (fileName == null || fileName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name is required.");
        }
        if (storageKey == null || storageKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Storage key is required.");
        }

        String typeClean = (documentType != null && VALID_DOC_TYPES.contains(documentType.toUpperCase())) ? documentType.toUpperCase() : "OTHER";

        Shipment shipment = shipmentId != null ? shipmentRepository.findById(shipmentId).orElse(null) : null;
        CustomsClearance clearance = customsClearanceId != null ? customsClearanceRepository.findById(customsClearanceId).orElse(null) : null;
        User activeUser = findUserByEmail(username);

        Document doc = new Document(shipment, clearance, typeClean, fileName.trim(), storageKey.trim(),
                mimeType != null ? mimeType.trim() : "application/pdf",
                sizeBytes != null ? sizeBytes : 1024L, null, activeUser);

        return documentRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public Resource loadDocumentResource(Long documentId, String username) {
        Document doc = getById(documentId);
        return storageService.loadFileAsResource(doc.getStorageKey());
    }

    public void deleteDocument(Long id, String username) {
        Document doc = getById(id);
        User activeUser = findUserByEmail(username);

        // Remove file from disk
        storageService.deleteFile(doc.getStorageKey());

        // Remove record
        documentRepository.delete(doc);

        auditLogRepository.save(new AuditLog(activeUser, "DELETE_DOCUMENT", "Document", id,
                "{\"fileName\":\"" + doc.getFileName() + "\"}"));
    }

    private User findUserByEmail(String username) {
        if (username == null) return null;
        return userRepository.findByEmailIgnoreCase(username).orElse(null);
    }
}
