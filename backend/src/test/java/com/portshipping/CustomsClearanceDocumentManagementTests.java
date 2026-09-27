package com.portshipping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portshipping.entity.*;
import com.portshipping.repository.*;
import com.portshipping.service.DocumentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CustomsClearanceDocumentManagementTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private PortRepository portRepository;
    @Autowired private VesselRepository vesselRepository;
    @Autowired private CustomsClearanceRepository customsRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DocumentStorageService storageService;

    private Shipment testShipment;
    private User testAdmin;

    @BeforeEach
    void setUp() {
        testAdmin = userRepository.findByEmailIgnoreCase("admin@port.local")
                .orElseGet(() -> {
                    User u = new User("Admin Officer", "admin@port.local", "encodedpass");
                    u.setRole("ADMIN");
                    return userRepository.save(u);
                });

        Port portA = portRepository.save(new Port("Customs Hub North", "CHN01", "Rotterdam"));
        Port portB = portRepository.save(new Port("Customs Hub South", "CHS02", "Singapore"));
        Vessel vessel = vesselRepository.save(new Vessel("MV Customs Express", "IMO9977551", "CONTAINER", "Panama", 12000, "Global Shipping", 300.0, 14.0, "ACTIVE"));

        testShipment = shipmentRepository.save(new Shipment(
                "TRK-CUST-2026-001", "REF-CUST-2026-001", "Rotterdam", "Singapore", "TechCorp Ltd", "Asia Import Co",
                LocalDate.now().plusDays(10), "IN_TRANSIT", 5, vessel
        ));
    }

    @Test
    @WithMockUser(username = "admin@port.local", roles = {"ADMIN"})
    void testFileCustomsDeclarationSuccess() throws Exception {
        Map<String, Object> req = Map.of(
                "shipmentId", testShipment.getId(),
                "declarationNumber", "DEC-2026-88990",
                "status", "PENDING",
                "documentDetails", "HS Code: 8471.30 - Laptops and Computers"
        );

        mockMvc.perform(post("/api/customs-clearances")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.declarationNumber", is("DEC-2026-88990")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.shipment.id", is(testShipment.getId().intValue())));
    }

    @Test
    @WithMockUser(username = "admin@port.local", roles = {"ADMIN"})
    void testFileDuplicateDeclarationNumberReturnsConflict() throws Exception {
        customsRepository.save(new CustomsClearance(testShipment, "DEC-DUP-1001", "PENDING", "Initial Filing"));

        Map<String, Object> req = Map.of(
                "shipmentId", testShipment.getId(),
                "declarationNumber", "DEC-DUP-1001",
                "status", "PENDING"
        );

        mockMvc.perform(post("/api/customs-clearances")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin@port.local", roles = {"ADMIN"})
    void testUpdateCustomsStatusApprovalSetsClearedTimestamp() throws Exception {
        CustomsClearance clearance = customsRepository.save(new CustomsClearance(testShipment, "DEC-APP-2002", "IN_REVIEW", "Inspection in progress"));

        Map<String, Object> req = Map.of(
                "status", "APPROVED",
                "remarks", "All tariff duties verified and paid in full."
        );

        mockMvc.perform(put("/api/customs-clearances/" + clearance.getId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.clearedAt", notNullValue()))
                .andExpect(jsonPath("$.remarks", is("All tariff duties verified and paid in full.")));
    }

    @Test
    @WithMockUser(username = "admin@port.local", roles = {"ADMIN"})
    void testRejectCustomsStatusWithoutRemarksReturnsBadRequest() throws Exception {
        CustomsClearance clearance = customsRepository.save(new CustomsClearance(testShipment, "DEC-REJ-3003", "IN_REVIEW", "Inspection in progress"));

        Map<String, Object> req = Map.of(
                "status", "REJECTED",
                "remarks", ""
        );

        mockMvc.perform(put("/api/customs-clearances/" + clearance.getId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@port.local", roles = {"ADMIN"})
    void testRejectCustomsStatusWithRemarksSuccess() throws Exception {
        CustomsClearance clearance = customsRepository.save(new CustomsClearance(testShipment, "DEC-REJ-4004", "IN_REVIEW", "Inspection in progress"));

        Map<String, Object> req = Map.of(
                "status", "REJECTED",
                "remarks", "Discrepancy in commercial invoice valuation vs physical cargo weight."
        );

        mockMvc.perform(put("/api/customs-clearances/" + clearance.getId() + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")))
                .andExpect(jsonPath("$.remarks", containsString("Discrepancy in commercial invoice")));
    }

    @Test
    @WithMockUser(username = "admin@port.local", roles = {"ADMIN"})
    void testMultipartDocumentUploadAndDownloadSuccess() throws Exception {
        CustomsClearance clearance = customsRepository.save(new CustomsClearance(testShipment, "DEC-DOC-5005", "PENDING", "Docs Attached"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Bill_Of_Lading_2026.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "Sample PDF Content for Digital Port Test Suite".getBytes()
        );

        String responseStr = mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("documentType", "BILL_OF_LADING")
                        .param("shipmentId", String.valueOf(testShipment.getId()))
                        .param("customsClearanceId", String.valueOf(clearance.getId()))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.fileName", is("Bill_Of_Lading_2026.pdf")))
                .andExpect(jsonPath("$.documentType", is("BILL_OF_LADING")))
                .andExpect(jsonPath("$.checksumSha256", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        Document doc = objectMapper.readValue(responseStr, Document.class);

        // Verify Download
        mockMvc.perform(get("/api/documents/" + doc.getId() + "/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("filename=\"Bill_Of_Lading_2026.pdf\"")))
                .andExpect(content().string("Sample PDF Content for Digital Port Test Suite"));
    }

    @Test
    @WithMockUser(username = "admin@port.local", roles = {"ADMIN"})
    void testOversizedDocumentUploadReturnsPayloadTooLarge() throws Exception {
        byte[] largeBytes = new byte[11 * 1024 * 1024]; // 11 MB > 10MB limit
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "oversized_cargo_manifest.pdf", MediaType.APPLICATION_PDF_VALUE, largeBytes
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(largeFile)
                        .param("documentType", "CARGO_MANIFEST")
                        .with(csrf()))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    @WithMockUser(username = "admin@port.local", roles = {"ADMIN"})
    void testDisallowedExtensionUploadReturnsBadRequest() throws Exception {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file", "malicious_script.exe", "application/x-msdownload", "binary content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(exeFile)
                        .param("documentType", "OTHER")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@port.local", roles = {"ADMIN"})
    void testPathTraversalAttemptInFilenamePrevented() throws Exception {
        MockMultipartFile traversalFile = new MockMultipartFile(
                "file", "../../../etc/passwd", MediaType.TEXT_PLAIN_VALUE, "malicious data".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(traversalFile)
                        .param("documentType", "OTHER")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "viewer@port.local", roles = {"VIEWER"})
    void testViewerRoleCannotFileDeclarationOrUploadDocument() throws Exception {
        Map<String, Object> req = Map.of(
                "shipmentId", testShipment.getId(),
                "declarationNumber", "DEC-VIEWER-99",
                "status", "PENDING"
        );

        mockMvc.perform(post("/api/customs-clearances")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "data".getBytes());

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("documentType", "BILL_OF_LADING")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
