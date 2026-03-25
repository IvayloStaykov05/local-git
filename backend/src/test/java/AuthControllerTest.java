import com.example.project.backend.config.JwtService;
import com.example.project.backend.controller.AuthController;
import com.example.project.backend.dto.user.request.UserLoginRequest;
import com.example.project.backend.dto.user.request.UserRegisterRequest;
import com.example.project.backend.dto.user.response.UserLoginResponse;
import com.example.project.backend.dto.user.response.UserRegisterResponse;
import com.example.project.backend.model.entity.User;
import com.example.project.backend.model.enums.SystemRole;
import com.example.project.backend.repository.UserRepository;
import com.example.project.backend.service.CustomUserDetailsService;
import com.example.project.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private AuthController authController;

    @Test
    void shouldRegisterUserSuccessfully() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("ivan123");
        request.setFirstName("Ivan");
        request.setLastName("Petrov");
        request.setEmail("ivan@example.com");
        request.setPassword("password123");

        UserRegisterResponse serviceResponse = new UserRegisterResponse(
                1L,
                "ivan123",
                "ivan@example.com",
                "User registered successfully"
        );

        when(userService.register(request)).thenReturn(serviceResponse);

        ResponseEntity<UserRegisterResponse> response = authController.register(request);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ivan123", response.getBody().getUsername());
        assertEquals("ivan@example.com", response.getBody().getEmail());
        assertEquals("User registered successfully", response.getBody().getMessage());

        verify(userService).register(request);
    }

    @Test
    void shouldLoginSuccessfullyWithUsername() {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsernameOrEmail("ivan123");
        request.setPassword("password123");

        User user = User.builder()
                .username("ivan123")
                .email("ivan@example.com")
                .password("encodedPassword")
                .systemRole(SystemRole.USER)
                .build();

        when(userRepository.findByUsername("ivan123")).thenReturn(Optional.of(user));
        when(customUserDetailsService.loadUserByUsername("ivan123")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        ResponseEntity<UserLoginResponse> response = authController.login(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("jwt-token", response.getBody().getToken());
        assertEquals("Bearer", response.getBody().getTokenType());
        assertEquals("ivan123", response.getBody().getUsername());
        assertEquals("ivan@example.com", response.getBody().getEmail());
        assertEquals(SystemRole.USER, response.getBody().getSystemRole());
        assertEquals("Login successful", response.getBody().getMessage());

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("ivan123", "password123")
        );
        verify(userRepository).findByUsername("ivan123");
        verify(userRepository, never()).findByEmail("ivan123");
        verify(customUserDetailsService).loadUserByUsername("ivan123");
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    void shouldLoginSuccessfullyWithEmail() {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsernameOrEmail("ivan@example.com");
        request.setPassword("password123");

        User user = User.builder()
                .username("ivan123")
                .email("ivan@example.com")
                .password("encodedPassword")
                .systemRole(SystemRole.USER)
                .build();

        when(userRepository.findByUsername("ivan@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(user));
        when(customUserDetailsService.loadUserByUsername("ivan123")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        ResponseEntity<UserLoginResponse> response = authController.login(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("jwt-token", response.getBody().getToken());
        assertEquals("Bearer", response.getBody().getTokenType());
        assertEquals("ivan123", response.getBody().getUsername());
        assertEquals("ivan@example.com", response.getBody().getEmail());
        assertEquals(SystemRole.USER, response.getBody().getSystemRole());
        assertEquals("Login successful", response.getBody().getMessage());

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("ivan@example.com", "password123")
        );
        verify(userRepository).findByUsername("ivan@example.com");
        verify(userRepository).findByEmail("ivan@example.com");
        verify(customUserDetailsService).loadUserByUsername("ivan123");
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    void shouldThrowWhenUserIsNotFoundDuringLogin() {
        UserLoginRequest request = new UserLoginRequest();
        request.setUsernameOrEmail("missingUser");
        request.setPassword("password123");

        when(userRepository.findByUsername("missingUser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("missingUser")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authController.login(request)
        );

        assertEquals("User not found", exception.getMessage());

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("missingUser", "password123")
        );
        verify(userRepository).findByUsername("missingUser");
        verify(userRepository).findByEmail("missingUser");
        verify(customUserDetailsService, never()).loadUserByUsername(anyString());
        verify(jwtService, never()).generateToken(any());
    }
}