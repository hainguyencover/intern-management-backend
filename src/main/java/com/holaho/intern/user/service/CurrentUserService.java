package com.holaho.intern.user.service;

import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUserEntity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); // thÆ°á»ng lÃ  email/username
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Current user not found by email=" + email));
    }
}

