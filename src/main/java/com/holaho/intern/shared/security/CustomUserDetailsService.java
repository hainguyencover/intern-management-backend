package com.holaho.intern.shared.security;

import com.holaho.intern.user.entity.Permission;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final com.holaho.intern.repository.UniversityUserRepository universityUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = email.trim().toLowerCase();
        log.info("Loading user by email: {}", normalized);

        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalized));

        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add role-based authorities (ROLE_ADMIN, ROLE_HR, etc.)
        for (Role role : user.getRoles()) {
            String code = role.getCode() != null ? role.getCode() : "";
            String roleAuthority = code.toUpperCase().startsWith("ROLE_") ? code : "ROLE_" + code;
            authorities.add(new SimpleGrantedAuthority(roleAuthority));

            // Add permissions from role
            if (role.getPermissions() != null) {
                for (Permission permission : role.getPermissions()) {
                    authorities.add(new SimpleGrantedAuthority(permission.getCode()));
                }
            }
        }

        log.info("User {} has authorities: {}", normalized, authorities);

        Long universityId = universityUserRepository.findByUserId(user.getId())
                .map(uu -> uu.getUniversity().getId())
                .orElse(null);

        return new CustomUserDetails(user, universityId);
    }
}

