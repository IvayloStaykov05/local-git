import com.example.project.backend.dto.response.documentVersion.DeleteDocumentResponse;
import com.example.project.backend.dto.response.documentVersion.DeleteDocumentVersionResponse;
import com.example.project.backend.dto.response.user.UserActivationResponse;
import com.example.project.backend.dto.response.user.UserDeactivationResponse;
import com.example.project.backend.model.entity.Document;
import com.example.project.backend.model.entity.DocumentVersion;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.SystemRole;
import com.example.project.backend.repository.DocumentRepository;
import com.example.project.backend.repository.DocumentVersionRepository;
import com.example.project.backend.repository.UserRepository;
import com.example.project.backend.service.DocumentService;
import com.example.project.backend.service.DocumentVersionService;
import com.example.project.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    @Mock
    private UserService userService; // 🔥 ВАЖНО

    @InjectMocks
    private UserService realUserService;

    @InjectMocks
    private DocumentVersionService documentVersionService;

    @InjectMocks
    private DocumentService documentService;


    private User buildAdmin() {
        User admin = User.builder()
                .username("adminUser")
                .systemRole(SystemRole.ADMIN)
                .build();
        admin.setId(99L);
        return admin;
    }

    @Test
    void shouldDeactivateUserSuccessfully() {
        User admin = buildAdmin();

        User user = User.builder()
                .username("ivan")
                .active(true)
                .build();
        user.setId(1L);

        when(userRepository.findByUsername("adminUser")).thenReturn(Optional.of(admin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDeactivationResponse response = realUserService.deactivateUser(1L, "adminUser");

        assertEquals(1L, response.getUserId());
        assertEquals("ivan", response.getUsername());
        assertFalse(response.isActive());
        assertEquals("User account is deactivated successfully.", response.getMessage());
    }

    @Test
    void shouldActivateUserSuccessfully() {
        User admin = buildAdmin();

        User user = User.builder()
                .username("ivan")
                .active(false)
                .build();
        user.setId(1L);

        when(userRepository.findByUsername("adminUser")).thenReturn(Optional.of(admin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserActivationResponse response = realUserService.activateUser(1L, "adminUser");

        assertEquals(1L, response.getUserId());
        assertEquals("ivan", response.getUsername());
        assertTrue(response.isActive());
        assertEquals("User account is activated successfully.", response.getMessage());
    }

    @Test
    void shouldDeleteDocumentVersionSuccessfully() {
        User admin = buildAdmin();

        when(userService.getValidatedAdmin(eq("adminUser"), anyString())).thenReturn(admin);

        Document document = new Document();
        document.setId(10L);

        DocumentVersion parent = new DocumentVersion();
        parent.setId(100L);
        parent.setVersionNumber(1);
        parent.setDocument(document);

        DocumentVersion version = new DocumentVersion();
        version.setId(200L);
        version.setVersionNumber(2);
        version.setDocument(document);
        version.setParentVersion(parent);

        document.setActiveVersion(version);

        when(documentVersionRepository.findById(200L)).thenReturn(Optional.of(version));
        when(documentVersionRepository.countByDocument(document)).thenReturn(2L);
        when(documentVersionRepository.existsByParentVersion(version)).thenReturn(false);

        DeleteDocumentVersionResponse response =
                documentVersionService.deleteDocumentVersion(200L, "adminUser");

        assertEquals(200L, response.getVersionId());
        assertEquals(10L, response.getDocumentId());
        assertEquals(2, response.getVersionNumber());

        verify(documentRepository).save(document);
        verify(documentVersionRepository).delete(version);
    }

    @Test
    void shouldDeleteDocumentSuccessfully() {
        User admin = buildAdmin();

        when(userService.getValidatedAdmin(eq("adminUser"), anyString())).thenReturn(admin);

        Document document = new Document();
        document.setId(10L);
        document.setTitle("Project Plan");

        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));

        DeleteDocumentResponse response =
                documentService.deleteDocument(10L, "adminUser");

        assertEquals(10L, response.getDocumentId());
        assertEquals("Project Plan", response.getTitle());

        verify(documentRepository).delete(document);
    }

    @Test
    void shouldThrowWhenNonAdminTriesToDeactivateUser() {
        User user = User.builder()
                .username("user")
                .systemRole(SystemRole.USER)
                .build();

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> realUserService.deactivateUser(1L, "user")
        );

        assertEquals("Only admins can access this resource.", ex.getMessage());
    }

    @Test
    void shouldThrowWhenNonAdminTriesToActivateUser() {
        User user = User.builder()
                .username("user")
                .systemRole(SystemRole.USER)
                .build();

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> realUserService.activateUser(1L, "user")
        );

        assertEquals("Only admins can access this resource.", ex.getMessage());
    }

    @Test
    void shouldThrowWhenDeletingOnlyVersionOfDocument() {
        User admin = buildAdmin();

        when(userService.getValidatedAdmin(eq("adminUser"), anyString())).thenReturn(admin);

        Document document = new Document();
        document.setId(10L);

        DocumentVersion version = new DocumentVersion();
        version.setId(100L);
        version.setDocument(document);

        when(documentVersionRepository.findById(100L)).thenReturn(Optional.of(version));
        when(documentVersionRepository.countByDocument(document)).thenReturn(1L);

        assertThrows(
                IllegalArgumentException.class,
                () -> documentVersionService.deleteDocumentVersion(100L, "adminUser")
        );
    }

    @Test
    void shouldThrowWhenDeletingParentVersion() {
        User admin = buildAdmin();

        when(userService.getValidatedAdmin(eq("adminUser"), anyString())).thenReturn(admin);

        Document document = new Document();
        document.setId(10L);

        DocumentVersion version = new DocumentVersion();
        version.setId(100L);
        version.setDocument(document);

        when(documentVersionRepository.findById(100L)).thenReturn(Optional.of(version));
        when(documentVersionRepository.countByDocument(document)).thenReturn(2L);
        when(documentVersionRepository.existsByParentVersion(version)).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> documentVersionService.deleteDocumentVersion(100L, "adminUser")
        );
    }
}