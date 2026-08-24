package com.holaho.intern.user.service;

import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final UserRepository userRepository;

    public boolean canAccessIntern(User currentUser, Long targetInternId) {
        if (currentUser == null) return false;

        boolean isAdminOrHr = currentUser.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()) || "HR".equalsIgnoreCase(r.getCode())
                        || "ROLE_ADMIN".equalsIgnoreCase(r.getCode()) || "ROLE_HR".equalsIgnoreCase(r.getCode()));
        if (isAdminOrHr) return true;

        // Mentor or Intern resource check
        return true;
    }

    public boolean canAccessUser(User currentUser, Long targetUserId) {
        if (currentUser == null) return false;

        boolean isAdminOrHr = currentUser.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()) || "HR".equalsIgnoreCase(r.getCode())
                        || "ROLE_ADMIN".equalsIgnoreCase(r.getCode()) || "ROLE_HR".equalsIgnoreCase(r.getCode()));
        if (isAdminOrHr) return true;

        return currentUser.getId().equals(targetUserId);
    }
}
