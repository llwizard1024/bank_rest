package com.example.bankcards.service;

import com.example.bankcards.dto.user.CreateUserRequest;
import com.example.bankcards.dto.user.UpdateUserStatusRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.exception.UsernameAlreadyExistsException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_assignsRoleAndEncodesPassword() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("admin2");
        request.setPassword("password123");
        request.setRole("ADMIN");

        when(userRepository.existsByUsername("admin2")).thenReturn(false);
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(new Role(1L, "ADMIN")));
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(5L);
            return user;
        });

        var response = userService.createUser(request);

        assertEquals("admin2", response.getUsername());
        assertTrue(response.getRoles().contains("ADMIN"));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void createUser_rejectsDuplicateUsername() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("alice");
        request.setPassword("password123");
        request.setRole("USER");

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class, () -> userService.createUser(request));
    }

    @Test
    void updateUserStatus_disablesUser() {
        User user = User.builder().username("bob").password("encoded").enabled(true).build();
        user.setId(2L);

        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setEnabled(false);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        var response = userService.updateUserStatus(2L, request);

        assertFalse(response.isEnabled());
        assertFalse(user.isEnabled());
    }

    @Test
    void getUserById_throwsWhenMissing() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(404L));
    }
}
