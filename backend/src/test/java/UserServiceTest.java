import com.example.project.backend.dto.user.request.UserRegisterRequest;
import com.example.project.backend.dto.user.response.UserRegisterResponse;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.repository.UserRepository;
import com.example.project.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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
        assertEquals("User registered successfully", response.getMessage());

        verify(userRepository).existsByUsername("ivan123");
        verify(userRepository).existsByEmail("ivan@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
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
}