package com.holaho.intern.service.impl;

import com.holaho.intern.auth.service.AuthServiceImpl;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private User setupUser;

    @BeforeEach
    void setUp() {
        setupUser = new User();
        setupUser.setId(1L);
        setupUser.setEmail("test@example.com");
        setupUser.setPasswordHash("encoded_password");
        setupUser.setFullName("Test User");
    }

    @Test
    void testUserSetup() {
        assertEquals("test@example.com", setupUser.getEmail());
    }
}
