import com.example.project.backend.dto.request.user.ForgotPasswordRequest;
import com.example.project.backend.dto.request.user.UserRegisterRequest;
import com.example.project.backend.dto.response.user.AddMyInfoResponse;
import com.example.project.backend.dto.response.user.UpdateMyInfoResponse;
import com.example.project.backend.dto.response.user.UserProfileResponse;
import com.example.project.backend.dto.response.user.UserRegisterResponse;
import com.example.project.backend.dto.response.user.UserSearchResponse;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.SystemRole;
import com.example.project.backend.repository.UserRepository;
import com.example.project.backend.repository.VerificationTokenRepository;
import com.example.project.backend.service.EmailService;
import com.example.project.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldRegisterUserSuccessfully() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("ivan123");
        request.setFirstName("Ivan");
        request.setLastName("Petrov");
        request.setEmail("ivan@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("ivan123")).thenReturn(false);
        when(userRepository.existsByEmail("ivan@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .username("ivan123")
                .firstName("Ivan")
                .lastName("Petrov")
                .email("ivan@example.com")
                .password("encodedPassword")
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserRegisterResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals("ivan123", response.getUsername());
        assertEquals("ivan@example.com", response.getEmail());

        verify(userRepository).existsByUsername("ivan123");
        verify(userRepository).existsByEmail("ivan@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(verificationTokenRepository).save(any());
        verify(emailService).sendVerificationEmail(eq("ivan@example.com"), anyString());
    }

    @Test
    void shouldThrowWhenUsernameAlreadyExists() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("ivan123");
        request.setFirstName("Ivan");
        request.setLastName("Petrov");
        request.setEmail("ivan@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("ivan123")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(request)
        );

        assertEquals("Username already exists", exception.getMessage());

        verify(userRepository).existsByUsername("ivan123");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(verificationTokenRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("ivan123");
        request.setFirstName("Ivan");
        request.setLastName("Petrov");
        request.setEmail("ivan@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("ivan123")).thenReturn(false);
        when(userRepository.existsByEmail("ivan@example.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(request)
        );

        assertEquals("Email already exists", exception.getMessage());

        verify(userRepository).existsByUsername("ivan123");
        verify(userRepository).existsByEmail("ivan@example.com");
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(verificationTokenRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void shouldEncodePasswordBeforeSavingUser() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("ivan123");
        request.setFirstName("Ivan");
        request.setLastName("Petrov");
        request.setEmail("ivan@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("ivan123")).thenReturn(false);
        when(userRepository.existsByEmail("ivan@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .username("ivan123")
                .firstName("Ivan")
                .lastName("Petrov")
                .email("ivan@example.com")
                .password("encodedPassword")
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        userService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();

        assertEquals("ivan123", capturedUser.getUsername());
        assertEquals("Ivan", capturedUser.getFirstName());
        assertEquals("Petrov", capturedUser.getLastName());
        assertEquals("ivan@example.com", capturedUser.getEmail());
        assertEquals("encodedPassword", capturedUser.getPassword());
    }

    @Test
    void shouldForgotPasswordSuccessfully() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setUsernameOrEmail("ivan123");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        User user = User.builder()
                .username("ivan123")
                .email("ivan@example.com")
                .password("oldPassword")
                .build();

        when(userRepository.findByUsername("ivan123")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

        String response = userService.forgotPassword(request);

        assertEquals("Password changed successfully", response);
        assertEquals("encodedNewPassword", user.getPassword());

        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowWhenForgotPasswordPasswordsDoNotMatch() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setUsernameOrEmail("ivan123");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("differentPassword");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.forgotPassword(request)
        );

        assertEquals("Passwords do not match", exception.getMessage());
        verify(userRepository, never()).findByUsername(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldSearchUsersSuccessfully() {
        User user1 = User.builder()
                .username("ivan123")
                .firstName("Ivan")
                .lastName("Petrov")
                .email("ivan@example.com")
                .build();
        user1.setId(1L);

        User user2 = User.builder()
                .username("maria456")
                .firstName("Maria")
                .lastName("Ivanova")
                .email("maria@example.com")
                .build();
        user2.setId(2L);

        when(userRepository.searchUsers("iv")).thenReturn(List.of(user1, user2));

        List<UserSearchResponse> response = userService.searchUsers("iv");

        assertEquals(2, response.size());
        assertEquals("ivan123", response.get(0).getUsername());
        assertEquals("Maria", response.get(1).getFirstName());
    }

    @Test
    void shouldGetUserProfileSuccessfully() {
        User loggedUser = User.builder()
                .username("ownerUser")
                .build();

        User targetUser = User.builder()
                .username("maria")
                .firstName("Maria")
                .lastName("Ivanova")
                .email("maria@example.com")
                .systemRole(SystemRole.USER)
                .myInfo("About Maria")
                .build();
        targetUser.setId(2L);

        when(userRepository.findByUsername("ownerUser")).thenReturn(Optional.of(loggedUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));

        UserProfileResponse response = userService.getUserProfile(2L, "ownerUser");

        assertEquals(2L, response.getId());
        assertEquals("maria", response.getUsername());
        assertEquals("Maria", response.getFirstName());
        assertEquals("Ivanova", response.getLastName());
        assertEquals("maria@example.com", response.getEmail());
        assertEquals(SystemRole.USER, response.getSystemRole());
        assertEquals("About Maria", response.getMyInfo());
    }

    @Test
    void shouldAddMyInfoSuccessfully() {
        User user = User.builder()
                .username("ivan123")
                .build();
        user.setId(1L);

        when(userRepository.findByUsername("ivan123")).thenReturn(Optional.of(user));

        AddMyInfoResponse response = userService.addMyInfo("My personal info", "ivan123");

        assertEquals(1L, response.getId());
        assertEquals("ivan123", response.getUsername());
        assertEquals("Personal information added successfully", response.getMessage());
        assertEquals("My personal info", user.getMyInfo());
    }

    @Test
    void shouldReturnMyInfoSuccessfully() {
        User user = User.builder()
                .username("ivan123")
                .myInfo("My personal info")
                .build();

        when(userRepository.findByUsername("ivan123")).thenReturn(Optional.of(user));

        String response = userService.getMyInfo("ivan123");

        assertEquals("My personal info", response);
    }

    @Test
    void shouldThrowWhenMyInfoDoesNotExist() {
        User user = User.builder()
                .username("ivan123")
                .myInfo(null)
                .build();

        when(userRepository.findByUsername("ivan123")).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getMyInfo("ivan123")
        );

        assertEquals("No personal information found", exception.getMessage());
    }

    @Test
    void shouldUpdateMyInfoSuccessfully() {
        User user = User.builder()
                .username("ivan123")
                .myInfo("Old info")
                .build();
        user.setId(1L);

        when(userRepository.findByUsername("ivan123")).thenReturn(Optional.of(user));

        UpdateMyInfoResponse response = userService.updateMyInfo("Updated info", "ivan123");

        assertEquals(1L, response.getId());
        assertEquals("ivan123", response.getUsername());
        assertEquals("Personal information updated successfully", response.getMessage());
        assertEquals("Updated info", user.getMyInfo());
    }

    @Test
    void shouldThrowWhenUpdatingMyInfoWithoutExistingInfo() {
        User user = User.builder()
                .username("ivan123")
                .myInfo(null)
                .build();

        when(userRepository.findByUsername("ivan123")).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateMyInfo("Updated info", "ivan123")
        );

        assertEquals("You must add personal information before you update it", exception.getMessage());
    }

    @Test
    void shouldGetAllUsersSuccessfullyWhenLoggedUserIsAdmin() {
        User admin = User.builder()
                .username("adminUser")
                .systemRole(SystemRole.ADMIN)
                .build();

        User user1 = User.builder()
                .username("ivan")
                .firstName("Ivan")
                .lastName("Petrov")
                .email("ivan@example.com")
                .build();
        user1.setId(1L);

        User user2 = User.builder()
                .username("maria")
                .firstName("Maria")
                .lastName("Ivanova")
                .email("maria@example.com")
                .build();
        user2.setId(2L);

        when(userRepository.findByUsername("adminUser")).thenReturn(Optional.of(admin));
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<UserSearchResponse> response = userService.getAllUsers("adminUser");

        assertEquals(2, response.size());
        assertEquals("ivan", response.get(0).getUsername());
        assertEquals("maria", response.get(1).getUsername());
    }

    @Test
    void shouldThrowWhenNonAdminTriesToGetAllUsers() {
        User user = User.builder()
                .username("normalUser")
                .systemRole(SystemRole.USER)
                .build();

        when(userRepository.findByUsername("normalUser")).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getAllUsers("normalUser")
        );

        assertEquals("You don't have access to this information", exception.getMessage());
        verify(userRepository, never()).findAll();
    }

}