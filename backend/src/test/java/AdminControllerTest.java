import com.example.project.backend.controller.AdminController;
import com.example.project.backend.dto.request.user.UserActivationRequest;
import com.example.project.backend.dto.request.user.UserDeactivationRequest;
import com.example.project.backend.dto.response.documentVersion.DeleteDocumentResponse;
import com.example.project.backend.dto.response.documentVersion.DeleteDocumentVersionResponse;
import com.example.project.backend.dto.response.user.UserActivationResponse;
import com.example.project.backend.dto.response.user.UserDeactivationResponse;
import com.example.project.backend.service.AdminService;
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
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminController adminController;

    @Test
    void shouldDeactivateUserSuccessfully() {
        UserDeactivationRequest request = new UserDeactivationRequest();
        request.setUserId(5L);

        UserDeactivationResponse serviceResponse = new UserDeactivationResponse(
                5L,
                "ivan123",
                false,
                "User account is deactivated successfully. "
        );

        when(authentication.getName()).thenReturn("adminUser");
        when(adminService.deactivateUser(5L, "adminUser")).thenReturn(serviceResponse);

        ResponseEntity<UserDeactivationResponse> response =
                adminController.deactivateUser(request, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5L, response.getBody().getUserId());
        assertEquals("ivan123", response.getBody().getUsername());
        assertFalse(response.getBody().isActive());
        assertEquals("User account is deactivated successfully. ", response.getBody().getMessage());

        verify(authentication).getName();
        verify(adminService).deactivateUser(5L, "adminUser");
    }

    @Test
    void shouldActivateUserSuccessfully() {
        UserActivationRequest request = new UserActivationRequest();
        request.setUserId(5L);

        UserActivationResponse serviceResponse = new UserActivationResponse(
                5L,
                "ivan123",
                true,
                "User account is activated successfully."
        );

        when(authentication.getName()).thenReturn("adminUser");
        when(adminService.activateUser(5L, "adminUser")).thenReturn(serviceResponse);

        ResponseEntity<UserActivationResponse> response =
                adminController.activateUser(request, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5L, response.getBody().getUserId());
        assertEquals("ivan123", response.getBody().getUsername());
        assertTrue(response.getBody().isActive());
        assertEquals("User account is activated successfully.", response.getBody().getMessage());

        verify(authentication).getName();
        verify(adminService).activateUser(5L, "adminUser");
    }

    @Test
    void shouldDeleteDocumentVersionSuccessfully() {
        DeleteDocumentVersionResponse serviceResponse = new DeleteDocumentVersionResponse(
                11L,
                7L,
                3,
                "Document version deleted successfully."
        );

        when(authentication.getName()).thenReturn("adminUser");
        when(adminService.deleteDocumentVersion(11L, "adminUser")).thenReturn(serviceResponse);

        ResponseEntity<DeleteDocumentVersionResponse> response =
                adminController.deleteDocumentVersion(11L, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(11L, response.getBody().getVersionId());
        assertEquals(7L, response.getBody().getDocumentId());
        assertEquals(3, response.getBody().getVersionNumber());
        assertEquals("Document version deleted successfully.", response.getBody().getMessage());

        verify(authentication).getName();
        verify(adminService).deleteDocumentVersion(11L, "adminUser");
    }

    @Test
    void shouldDeleteDocumentSuccessfully() {
        DeleteDocumentResponse serviceResponse = new DeleteDocumentResponse(
                7L,
                "Project Plan",
                "Document deleted successfully."
        );

        when(authentication.getName()).thenReturn("adminUser");
        when(adminService.deleteDocument(7L, "adminUser")).thenReturn(serviceResponse);

        ResponseEntity<DeleteDocumentResponse> response =
                adminController.deleteDocument(7L, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(7L, response.getBody().getDocumentId());
        assertEquals("Project Plan", response.getBody().getTitle());
        assertEquals("Document deleted successfully.", response.getBody().getMessage());

        verify(authentication).getName();
        verify(adminService).deleteDocument(7L, "adminUser");
    }
}