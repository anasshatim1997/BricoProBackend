package com.bricopro.config;

import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminBootstrapRunner")
class AdminBootstrapRunnerTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AdminBootstrapRunner runner;

    @BeforeEach
    void setup() throws Exception {
        setField("adminFirstName", "Admin");
        setField("adminLastName", "BricoPro");
    }

    private void setField(String name, String value) throws Exception {
        Field f = AdminBootstrapRunner.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(runner, value);
    }

    @Test
    @DisplayName("does nothing when adminEmail is blank")
    void doesNothingWhenEmailBlank() throws Exception {
        setField("adminEmail", "");
        setField("adminPassword", "somePassword123");

        runner.run();

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("does nothing when adminPassword is blank")
    void doesNothingWhenPasswordBlank() throws Exception {
        setField("adminEmail", "admin@bricopro.ma");
        setField("adminPassword", "");

        runner.run();

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("does nothing when a user with that email already exists")
    void doesNothingWhenEmailAlreadyExists() throws Exception {
        setField("adminEmail", "admin@bricopro.ma");
        setField("adminPassword", "somePassword123");
        when(userRepository.existsByEmail("admin@bricopro.ma")).thenReturn(true);

        runner.run();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("creates the admin user with an encoded password, ADMIN role, and verified status")
    void createsAdminUserOnFirstBoot() throws Exception {
        setField("adminEmail", "admin@bricopro.ma");
        setField("adminPassword", "plainTextPassword");
        when(userRepository.existsByEmail("admin@bricopro.ma")).thenReturn(false);
        when(passwordEncoder.encode("plainTextPassword")).thenReturn("$2a$hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        runner.run();

        verify(userRepository).save(argThat(u ->
                u.getEmail().equals("admin@bricopro.ma")
                        && u.getPasswordHash().equals("$2a$hashed")
                        && u.getRole() == Role.ADMIN
                        && u.isVerified()));
    }

    @Test
    @DisplayName("never stores the plain-text password on the created user")
    void neverStoresPlainTextPassword() throws Exception {
        setField("adminEmail", "admin@bricopro.ma");
        setField("adminPassword", "plainTextPassword");
        when(userRepository.existsByEmail("admin@bricopro.ma")).thenReturn(false);
        when(passwordEncoder.encode("plainTextPassword")).thenReturn("$2a$hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        runner.run();

        verify(userRepository).save(argThat(u -> !u.getPasswordHash().equals("plainTextPassword")));
        verify(passwordEncoder).encode("plainTextPassword");
    }
}
