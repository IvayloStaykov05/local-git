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
import com.example.project.backend.service.AdminService;
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

    @InjectMocks
    private AdminService adminService;

    @Test
    void shouldDeactivateUserSuccessfully() {
        User admin = User.builder()
                .username("adminUser")
                .systemRole(SystemRole.ADMIN)
                .build();

        User user = User.builder()
                .username("ivan")
                .active(true)
                .build();
        user.setId(1L);

        when(userRepository.findByUsername("adminUser")).thenReturn(Optional.of(admin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDeactivationResponse response = adminService.deactivateUser(1L, "adminUser");

        assertEquals(1L, response.getUserId());
        assertEquals("ivan", response.getUsername());
        assertFalse(response.isActive());
        assertEquals("User account is deactivated successfully. ", response.getMessage());
        assertFalse(user.isActive());
    }

    @Test
    void shouldActivateUserSuccessfully() {
        User admin = User.builder()
                .username("adminUser")
                .systemRole(SystemRole.ADMIN)
                .build();

        User user = User.builder()
                .username("ivan")
                .active(false)
                .build();
        user.setId(1L);

        when(userRepository.findByUsername("adminUser")).thenReturn(Optional.of(admin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserActivationResponse response = adminService.activateUser(1L, "adminUser");

        assertEquals(1L, response.getUserId());
        assertEquals("ivan", response.getUsername());
        assertTrue(response.isActive());
        assertEquals("User account is activated successfully.", response.getMessage());
        assertTrue(user.isActive());
    }

    @Test
    void shouldDeleteDocumentVersionSuccessfully() {
        User admin = User.builder()
                .username("adminUser")
                .systemRole(SystemRole.ADMIN)
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .build();
        document.setId(10L);

        DocumentVersion parentVersion = DocumentVersion.builder()
                .versionNumber(1)
                .document(document)
                .build();
        parentVersion.setId(100L);

        DocumentVersion version = DocumentVersion.builder()
                .versionNumber(2)
                .document(document)
                .parentVersion(parentVersion)
                .build();
        version.setId(200L);

        document.setActiveVersion(version);

        when(userRepository.findByUsername("adminUser")).thenReturn(Optional.of(admin));
        when(documentVersionRepository.findById(200L)).thenReturn(Optional.of(version));
        when(documentVersionRepository.countByDocument(document)).thenReturn(2L);
        when(documentVersionRepository.existsByParentVersion(version)).thenReturn(false);

        DeleteDocumentVersionResponse response = adminService.deleteDocumentVersion(200L, "adminUser");

        assertEquals(200L, response.getVersionId());
        assertEquals(10L, response.getDocumentId());
        assertEquals(2, response.getVersionNumber());
        assertEquals("Document version deleted successfully.", response.getMessage());

        assertEquals(parentVersion, document.getActiveVersion());
        verify(documentRepository).save(document);
        verify(documentVersionRepository).delete(version);
    }

    @Test
    void shouldDeleteDocumentSuccessfully() {
        User admin = User.builder()
                .username("adminUser")
                .systemRole(SystemRole.ADMIN)
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .build();
        document.setId(10L);

        when(userRepository.findByUsername("adminUser")).thenReturn(Optional.of(admin));
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));

        DeleteDocumentResponse response = adminService.deleteDocument(10L, "adminUser");

        assertEquals(10L, response.getDocumentId());
        assertEquals("Project Plan", response.getTitle());
        assertEquals("Document deleted successfully.", response.getMessage());

        verify(documentRepository).delete(document);
    }

    @Test
    void shouldThrowWhenNonAdminTriesToDeactivateUser() {
        User user = User.builder()
                .username("normalUser")
                .systemRole(SystemRole.USER)
                .build();

        when(userRepository.findByUsername("normalUser")).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.deactivateUser(1L, "normalUser")
        );

        assertEquals("Only admins can deactivate users.", exception.getMessage());
    }

    @Test
    void shouldThrowWhenNonAdminTriesToActivateUser() {
        User user = User.builder()
                .username("ivan")
                .systemRole(SystemRole.USER)
                .build();

        when(userRepository.findByUsername("ivan")).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.activateUser(1L, "ivan")
        );

        assertEquals(" Only admins can activate users.", exception.getMessage());
    }

    @Test
    void shouldThrowWhenAdminNotFoundWhileDeactivatingUser() {
        when(userRepository.findByUsername("missingAdmin")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.deactivateUser(1L, "missingAdmin")
        );

        assertEquals("Admin not found.", exception.getMessage());
    }

    @Test
    void shouldThrowWhenUserToDeactivateIsNotFound() {
        User admin = User.builder()
                .username("adminUser")
                .systemRole(SystemRole.ADMIN)
                .build();

        when(userRepository.findByUsername("adminUser")).thenReturn(Optional.of(admin));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.deactivateUser(1L, "adminUser")
        );

        assertEquals("User not found.", exception.getMessage());
    }

    @Test
    void shouldThrowWhenDeletingOnlyVersionOfDocument() {
        User admin = User.builder()
                .username("adminUser")
                .systemRole(SystemRole.ADMIN)
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .build();
        document.setId(10L);

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(1)
                .build();
        version.setId(100L);

        when(userRepository.findByUsername("adminUser")).thenReturn(Optional.of(admin));
        when(documentVersionRepository.findById(100L)).thenReturn(Optional.of(version));
        when(documentVersionRepository.countByDocument(document)).thenReturn(1L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.deleteDocumentVersion(100L, "adminUser")
        );

        assertEquals("Cannot delete the only version of a document. Delete the whole document instead.", exception.getMessage());

        verify(documentVersionRepository, never()).delete(any(DocumentVersion.class));
    }

    @Test
    void shouldThrowWhenDeletingParentVersion() {
        User admin = User.builder()
                .username("adminUser")
                .systemRole(SystemRole.ADMIN)
                .build();

        Document document = Document.builder()
                .title("Project Plan")
                .build();
        document.setId(10L);

        DocumentVersion version = DocumentVersion.builder()
                .document(document)
                .versionNumber(1)
                .build();
        version.setId(100L);

        when(userRepository.findByUsername("adminUser")).thenReturn(Optional.of(admin));
        when(documentVersionRepository.findById(100L)).thenReturn(Optional.of(version));
        when(documentVersionRepository.countByDocument(document)).thenReturn(2L);
        when(documentVersionRepository.existsByParentVersion(version)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.deleteDocumentVersion(100L, "adminUser")
        );

        assertEquals("Cannot delete a version that is parent of another version.", exception.getMessage());

        verify(documentVersionRepository, never()).delete(any(DocumentVersion.class));
    }

}