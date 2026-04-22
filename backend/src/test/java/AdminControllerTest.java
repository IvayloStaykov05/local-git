import com.example.project.backend.controller.admin.AdminDocumentController;
import com.example.project.backend.controller.admin.AdminDocumentVersionController;
import com.example.project.backend.controller.admin.AdminUserController;
import com.example.project.backend.dto.request.user.UserActivationRequest;
import com.example.project.backend.dto.request.user.UserDeactivationRequest;
import com.example.project.backend.dto.response.documentVersion.DeleteDocumentResponse;
import com.example.project.backend.dto.response.documentVersion.DeleteDocumentVersionResponse;
import com.example.project.backend.dto.response.user.UserActivationResponse;
import com.example.project.backend.dto.response.user.UserDeactivationResponse;
import com.example.project.backend.service.DocumentService;
import com.example.project.backend.service.DocumentVersionService;
import com.example.project.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.example.project.backend.dto.response.admin.AdminDocumentTableResponse;
import java.util.List;
import com.example.project.backend.dto.response.admin.AdminDocumentTableResponse;
import java.util.List;
import java.time.LocalDateTime;



import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private DocumentService documentService;

    @Mock
    private DocumentVersionService documentVersionService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminUserController adminUserController;

    @InjectMocks
    private AdminDocumentController adminDocumentController;

    @InjectMocks
    private AdminDocumentVersionController adminDocumentVersionController;

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
        when(userService.deactivateUser(5L, "adminUser")).thenReturn(serviceResponse);

        ResponseEntity<UserDeactivationResponse> response =
                adminUserController.deactivateUser(request, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5L, response.getBody().getUserId());
        assertEquals("ivan123", response.getBody().getUsername());
        assertFalse(response.getBody().isActive());
        assertEquals("User account is deactivated successfully. ", response.getBody().getMessage());

        verify(authentication).getName();
        verify(userService).deactivateUser(5L, "adminUser");
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
        when(userService.activateUser(5L, "adminUser")).thenReturn(serviceResponse);

        ResponseEntity<UserActivationResponse> response =
                adminUserController.activateUser(request, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5L, response.getBody().getUserId());
        assertEquals("ivan123", response.getBody().getUsername());
        assertTrue(response.getBody().isActive());
        assertEquals("User account is activated successfully.", response.getBody().getMessage());

        verify(authentication).getName();
        verify(userService).activateUser(5L, "adminUser");
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
        when(documentVersionService.deleteDocumentVersion(11L, "adminUser")).thenReturn(serviceResponse);

        ResponseEntity<DeleteDocumentVersionResponse> response =
                adminDocumentVersionController.deleteDocumentVersion(11L, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(11L, response.getBody().getVersionId());
        assertEquals(7L, response.getBody().getDocumentId());
        assertEquals(3, response.getBody().getVersionNumber());
        assertEquals("Document version deleted successfully.", response.getBody().getMessage());

        verify(authentication).getName();
        verify(documentVersionService).deleteDocumentVersion(11L, "adminUser");
    }

    @Test
    void shouldDeleteDocumentSuccessfully() {
        DeleteDocumentResponse serviceResponse = new DeleteDocumentResponse(
                7L,
                "Project Plan",
                "Document deleted successfully."
        );

        when(authentication.getName()).thenReturn("adminUser");
        when(documentService.deleteDocument(7L, "adminUser")).thenReturn(serviceResponse);

        ResponseEntity<DeleteDocumentResponse> response =
                adminDocumentController.deleteDocument(7L, authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(7L, response.getBody().getDocumentId());
        assertEquals("Project Plan", response.getBody().getTitle());
        assertEquals("Document deleted successfully.", response.getBody().getMessage());

        verify(authentication).getName();
        verify(documentService).deleteDocument(7L, "adminUser");
    }

    @Test
    void shouldGetAdminDocumentsSuccessfully() {
        AdminDocumentTableResponse tableResponse = mock(AdminDocumentTableResponse.class);

        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 20, 12, 0);

        when(tableResponse.getId()).thenReturn(10L);
        when(tableResponse.getTitle()).thenReturn("Project Plan");
        when(tableResponse.getOwnerUsername()).thenReturn("ownerUser");
        when(tableResponse.getVersionsCount()).thenReturn(2L);
        when(tableResponse.getCreatedAt()).thenReturn(createdAt);

        when(authentication.getName()).thenReturn("adminUser");
        when(documentService.getAdminDocuments("adminUser", "Project"))
                .thenReturn(List.of(tableResponse));

        ResponseEntity<List<AdminDocumentTableResponse>> response =
                adminDocumentController.getAdminDocuments("Project", authentication);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(10L, response.getBody().get(0).getId());
        assertEquals("Project Plan", response.getBody().get(0).getTitle());
        assertEquals("ownerUser", response.getBody().get(0).getOwnerUsername());
        assertEquals(2L, response.getBody().get(0).getVersionsCount());
        assertEquals(createdAt, response.getBody().get(0).getCreatedAt());

        verify(authentication).getName();
        verify(documentService).getAdminDocuments("adminUser", "Project");
    }
}