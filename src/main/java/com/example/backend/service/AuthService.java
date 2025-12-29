package com.example.backend.service;

import com.example.backend.dto.StoredFile;
import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.SignupRequest;
import com.example.backend.dto.response.JwtResponse;
import com.example.backend.entity.InternDocument;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.enums.DocumentType;
import com.example.backend.enums.UserStatus;
import com.example.backend.repository.InternDocumentRepository;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtTokenProvider;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    private final InternProfileRepository internProfileRepository;
    private final InternDocumentRepository internDocumentRepository;
    private final LocalStorageService localStorageService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       InternProfileRepository internProfileRepository,
                       InternDocumentRepository internDocumentRepository,
                       LocalStorageService localStorageService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.internProfileRepository = internProfileRepository;
        this.internDocumentRepository = internDocumentRepository;
        this.localStorageService = localStorageService;
    }

    @Transactional
    public JwtResponse authenticate(LoginRequest loginRequest) {
        String email = loginRequest.getEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword())
        );

        UserDetails principal = (UserDetails) authentication.getPrincipal();

        String jwt = tokenProvider.generateToken(Objects.requireNonNull(principal));
        User user = userRepository.findByEmail(email).orElseThrow();

        Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());

        return JwtResponse.builder()
                .token(jwt)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .build();
    }

    @Transactional
    public String register(SignupRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }

        if (request.getEndYear() < request.getStartYear()) {
            throw new IllegalArgumentException("endYear must be >= startYear");
        }

        // default role INTERN
        Role internRole = roleRepository.findByCode("INTERN")
                .orElseThrow(() -> new IllegalArgumentException("Role not found: INTERN"));

        // 1) User
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());          // nếu User có field phone
        user.setAddress(request.getAddress());      // nếu User có field address
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(internRole));
        user = userRepository.save(user);

        // 2) InternProfile
        InternProfile ip = new InternProfile();
        ip.setUser(user);
        ip.setPhone(request.getPhone());            // nếu InternProfile có phone
        ip.setAddress(request.getAddress());        // nếu InternProfile có address
        ip.setStudentCode(request.getStudentCode());
        ip.setUniversity(request.getUniversity());
        ip.setMajor(request.getMajor());

        // convert year -> LocalDate (01-01)
        ip.setDob(LocalDate.of(request.getDobYear(), 1, 1));
        ip.setStartDate(LocalDate.of(request.getStartYear(), 1, 1));
        ip.setEndDate(LocalDate.of(request.getEndYear(), 12, 31));

        internProfileRepository.save(ip);

        return "User registered successfully";
    }

}
