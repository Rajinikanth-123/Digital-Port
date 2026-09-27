package com.portshipping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portshipping.dto.CreateMessageRequest;
import com.portshipping.dto.CreateTicketRequest;
import com.portshipping.dto.SubmitFeedbackRequest;
import com.portshipping.entity.SupportTicket;
import com.portshipping.entity.User;
import com.portshipping.repository.FeedbackRepository;
import com.portshipping.repository.SupportTicketRepository;
import com.portshipping.repository.TicketMessageRepository;
import com.portshipping.repository.TicketStatusHistoryRepository;
import com.portshipping.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SupportTicketsHelpCenterSystemTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupportTicketRepository ticketRepository;

    @Autowired
    private TicketMessageRepository messageRepository;

    @Autowired
    private TicketStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User regularUser;
    private User supportAgent;
    private User otherUser;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        statusHistoryRepository.deleteAll();
        feedbackRepository.deleteAll();
        ticketRepository.deleteAll();

        regularUser = userRepository.findByEmailIgnoreCase("user_step19@portshipping.com").orElseGet(() -> {
            User u = new User("Regular Ticket User", "user_step19@portshipping.com", passwordEncoder.encode("password123"));
            u.setRole("VIEWER");
            return userRepository.save(u);
        });

        supportAgent = userRepository.findByEmailIgnoreCase("agent_step19@portshipping.com").orElseGet(() -> {
            User u = new User("Support Agent", "agent_step19@portshipping.com", passwordEncoder.encode("password123"));
            u.setRole("SHIPPING_AGENT");
            return userRepository.save(u);
        });

        otherUser = userRepository.findByEmailIgnoreCase("other_step19@portshipping.com").orElseGet(() -> {
            User u = new User("Other User", "other_step19@portshipping.com", passwordEncoder.encode("password123"));
            u.setRole("VIEWER");
            return userRepository.save(u);
        });
    }

    @Test
    @WithMockUser(username = "user_step19@portshipping.com", roles = "VIEWER")
    void testCreateTicket_Success() throws Exception {
        CreateTicketRequest req = new CreateTicketRequest();
        req.setSubject("Container Damaged on Arrival");
        req.setDescription("Container #CNTR-9901 showed structural dent on top seal.");
        req.setCategory("SHIPMENT");
        req.setPriority("HIGH");

        MockMultipartFile dataPart = new MockMultipartFile("data", "", "application/json", objectMapper.writeValueAsBytes(req));

        mockMvc.perform(multipart("/api/support/tickets")
                        .file(dataPart)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketReference", startsWith("TICK-")))
                .andExpect(jsonPath("$.subject", is("Container Damaged on Arrival")))
                .andExpect(jsonPath("$.category", is("SHIPMENT")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.createdByEmail", is("user_step19@portshipping.com")));
    }

    @Test
    @WithMockUser(username = "user_step19@portshipping.com", roles = "VIEWER")
    void testGetMyTickets_Isolation() throws Exception {
        SupportTicket ticket = ticketRepository.save(new SupportTicket("TICK-TEST-001", regularUser, "Test Subject", "Test Desc", "GENERAL", "MEDIUM"));

        mockMvc.perform(get("/api/support/tickets/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].ticketReference", is("TICK-TEST-001")));
    }

    @Test
    @WithMockUser(username = "other_step19@portshipping.com", roles = "VIEWER")
    void testGetTicketById_ForbiddenForOtherUser() throws Exception {
        SupportTicket ticket = ticketRepository.save(new SupportTicket("TICK-TEST-002", regularUser, "Private Subject", "Private Desc", "GENERAL", "MEDIUM"));

        mockMvc.perform(get("/api/support/tickets/" + ticket.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user_step19@portshipping.com", roles = "VIEWER")
    void testAddMessageToTicket_UserReply() throws Exception {
        SupportTicket ticket = ticketRepository.save(new SupportTicket("TICK-TEST-003", regularUser, "Need Invoice Update", "Need clarification", "BILLING", "MEDIUM"));

        CreateMessageRequest req = new CreateMessageRequest();
        req.setMessageBody("Here is additional context regarding payment receipt.");

        MockMultipartFile dataPart = new MockMultipartFile("data", "", "application/json", objectMapper.writeValueAsBytes(req));

        mockMvc.perform(multipart("/api/support/tickets/" + ticket.getId() + "/messages")
                        .file(dataPart)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageBody", containsString("additional context")))
                .andExpect(jsonPath("$.senderEmail", is("user_step19@portshipping.com")));
    }

    @Test
    @WithMockUser(username = "agent_step19@portshipping.com", roles = "SHIPPING_AGENT")
    void testStaffInternalNote_HiddenFromRegularUser() throws Exception {
        SupportTicket ticket = ticketRepository.save(new SupportTicket("TICK-TEST-004", regularUser, "Customs Hold Issue", "Customs clearance delay", "CUSTOMS", "HIGH"));

        // Staff adds internal note
        CreateMessageRequest internalReq = new CreateMessageRequest();
        internalReq.setMessageBody("INTERNAL NOTE: Verified port documents with customs inspector.");
        internalReq.setInternalNote(true);

        MockMultipartFile internalPart = new MockMultipartFile("data", "", "application/json", objectMapper.writeValueAsBytes(internalReq));

        mockMvc.perform(multipart("/api/support/tickets/" + ticket.getId() + "/messages")
                        .file(internalPart)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internalNote", is(true)));

        // Staff adds public reply
        CreateMessageRequest publicReq = new CreateMessageRequest();
        publicReq.setMessageBody("Hello! We are currently investigating your clearance hold with customs.");
        publicReq.setInternalNote(false);

        MockMultipartFile publicPart = new MockMultipartFile("data", "", "application/json", objectMapper.writeValueAsBytes(publicReq));

        mockMvc.perform(multipart("/api/support/tickets/" + ticket.getId() + "/messages")
                        .file(publicPart)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "agent_step19@portshipping.com", roles = "SHIPPING_AGENT")
    void testAdminAssignAndStatusUpdate() throws Exception {
        SupportTicket ticket = ticketRepository.save(new SupportTicket("TICK-TEST-005", regularUser, "Vessel Schedule Change", "Need schedule update", "VESSEL", "URGENT"));

        // Assign to agent
        mockMvc.perform(patch("/api/admin/support/tickets/" + ticket.getId() + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedToId\": " + supportAgent.getId() + ", \"note\": \"Assigned to support agent\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedToId", is(supportAgent.getId().intValue())));

        // Update status to RESOLVED
        mockMvc.perform(patch("/api/admin/support/tickets/" + ticket.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"RESOLVED\", \"note\": \"Issue resolved by support\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESOLVED")));
    }

    @Test
    @WithMockUser(username = "user_step19@portshipping.com", roles = "VIEWER")
    void testSubmitFeedback_Success() throws Exception {
        SubmitFeedbackRequest req = new SubmitFeedbackRequest();
        req.setRating(5);
        req.setCategory("SUPPORT");
        req.setFeedbackText("Excellent response time and support resolution!");

        mockMvc.perform(post("/api/support/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating", is(5)))
                .andExpect(jsonPath("$.category", is("SUPPORT")))
                .andExpect(jsonPath("$.reviewState", is("PENDING")));
    }

    @Test
    void testHelpArticlesSearch_PublicAccess() throws Exception {
        mockMvc.perform(get("/api/help/articles?search=customs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));

        mockMvc.perform(get("/api/help/faqs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }
}
