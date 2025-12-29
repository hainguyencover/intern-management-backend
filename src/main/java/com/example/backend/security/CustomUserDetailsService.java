package com.example.backend.security;

import com.example.backend.entity.User;
import com.example.backend.enums.UserStatus;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

        private final UserRepository userRepository;

        @Override
        @Transactional(readOnly = true)
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
                String normalized = email.trim().toLowerCase();
                log.info("Loading user by email: {}", normalized);

                User user = userRepository.findByEmail(normalized)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalized));

                Set<GrantedAuthority> authorities = user.getRoles().stream()
                                .map(role -> {
                                        String code = role.getCode() != null ? role.getCode() : "";
                                        String authority = code.toUpperCase().startsWith("ROLE_") ? code
                                                        : "ROLE_" + code;
                                        return new SimpleGrantedAuthority(authority);
                                })
                                .collect(Collectors.toSet());

                return org.springframework.security.core.userdetails.User.builder()
                                .username(user.getEmail())
                                .password(user.getPasswordHash())
                                .authorities(authorities)
                                .disabled(user.getStatus() != UserStatus.ACTIVE)
                                .build();
        }
}
