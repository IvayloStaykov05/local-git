import com.example.project.backend.controller.admin.AdminInvitationController;
import com.example.project.backend.dto.request.admin.CreateAdminProfileRequest;
import com.example.project.backend.dto.request.admin.InviteAdminRequest;
import com.example.project.backend.dto.response.admin.AdminInvitationResponse;
import com.example.project.backend.dto.response.admin.CreateAdminProfileResponse;
import com.example.project.backend.dto.response.invite.ActionResponse;
import com.example.project.backend.service.AdminInvitationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInvitationControllerTest {

    @Mock
    private AdminInvitationService adminInvitationService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminInvitationController adminInvitationController;

    @Test
    void shouldInviteToBecomeAdminSuccessfully() {
        InviteAdminRequest request = new InviteAdminRequest();
        request.setUsername("ivan123");

        AdminInvitationResponse serviceResponse = new AdminInvitationResponse(
                100L,
                "adminUser",
                "ivan123",
                "PENDING",
                "Admin invitation sent successfully"
        );

        when(authentication.getName()).thenReturn("adminUser");
        when(adminInvitationService.inviteToBecomeAdmin("adminUser", request)).thenReturn(serviceResponse);

        ResponseEntity<AdminInvitationResponse> response =
                adminInvitationController.inviteToBecomeAdmin(request, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(100L, response.getBody().getInvitationId());
        assertEquals("adminUser", response.getBody().getSenderUsername());
        assertEquals("ivan123", response.getBody().getRecipientUsername());
        assertEquals("PENDING", response.getBody().getStatus());
        assertEquals("Admin invitation sent successfully", response.getBody().getMessage());

        verify(authentication).getName();
        verify(adminInvitationService).inviteToBecomeAdmin("adminUser", request);
    }

    @Test
    void shouldAcceptAdminInvitationSuccessfully() {
        ActionResponse serviceResponse = new ActionResponse(
                "Admin invitation accepted successfully"
        );

        when(authentication.getName()).thenReturn("ivan123");
        when(adminInvitationService.acceptAdminInvitation(100L, "ivan123")).thenReturn(serviceResponse);

        ResponseEntity<ActionResponse> response =
                adminInvitationController.acceptAdminInvitation(100L, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Admin invitation accepted successfully", response.getBody().getMessage());

        verify(authentication).getName();
        verify(adminInvitationService).acceptAdminInvitation(100L, "ivan123");
    }

    @Test
    void shouldRejectAdminInvitationSuccessfully() {
        ActionResponse serviceResponse = new ActionResponse(
                "Admin invitation rejected successfully"
        );

        when(authentication.getName()).thenReturn("ivan123");
        when(adminInvitationService.rejectAdminInvitation(100L, "ivan123")).thenReturn(serviceResponse);

        ResponseEntity<ActionResponse> response =
                adminInvitationController.rejectAdminInvitation(100L, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Admin invitation rejected successfully", response.getBody().getMessage());

        verify(authentication).getName();
        verify(adminInvitationService).rejectAdminInvitation(100L, "ivan123");
    }

    @Test
    void shouldCreateAdminProfileSuccessfully() {
        CreateAdminProfileRequest request = new CreateAdminProfileRequest();
        request.setInvitationId(100L);
        request.setAdminUsername("ivan_admin");
        request.setAdminEmail("ivan.admin@example.com");
        request.setAdminPassword("secret123");

        CreateAdminProfileResponse serviceResponse = new CreateAdminProfileResponse(
                10L,
                2L,
                "ivan_admin",
                "ivan.admin@example.com",
                "Admin profile created successfully"
        );

        when(authentication.getName()).thenReturn("adminUser");
        when(adminInvitationService.createAdminProfile("adminUser", request)).thenReturn(serviceResponse);

        ResponseEntity<CreateAdminProfileResponse> response =
                adminInvitationController.createAdminProfile(request, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10L, response.getBody().getAdminProfileId());
        assertEquals(2L, response.getBody().getLinkedUserId());
        assertEquals("ivan_admin", response.getBody().getAdminUsername());
        assertEquals("ivan.admin@example.com", response.getBody().getAdminEmail());
        assertEquals("Admin profile created successfully", response.getBody().getMessage());

        verify(authentication).getName();
        verify(adminInvitationService).createAdminProfile("adminUser", request);
    }
}