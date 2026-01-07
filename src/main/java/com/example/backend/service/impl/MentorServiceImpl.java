package com.example.backend.service.impl;

import com.example.backend.dto.request.MentorCreateRequest;
import com.example.backend.dto.response.MentorResponse;
import com.example.backend.entity.Department;
import com.example.backend.entity.Mentor;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.enums.UserStatus;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.DepartmentRepository;
import com.example.backend.repository.MentorRepository;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.MentorService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class MentorServiceImpl implements MentorService {

    private final MentorRepository mentorRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public MentorServiceImpl(
            MentorRepository mentorRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.mentorRepository = mentorRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public MentorResponse createMentor(MentorCreateRequest req) {

        if (req.getEmail() == null || req.getEmail().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (req.getPassword() == null || req.getPassword().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (req.getFullName() == null || req.getFullName().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Full name is required");
        }

        String email = req.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists: " + email);
        }

        Role mentorRole = roleRepository.findByCode("MENTOR")
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Role not found: MENTOR"));

        Department dept = null;
        if (req.getDepartmentId() != null) {
            dept = departmentRepository.findById(req.getDepartmentId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "Department not found: " + req.getDepartmentId()
                    ));
        }

        User u = new User();
        u.setEmail(email);
        u.setFullName(req.getFullName().trim());
        u.setPhone(req.getPhone());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setStatus(UserStatus.ACTIVE);

        Set<Role> roles = new HashSet<>();
        roles.add(mentorRole);
        u.setRoles(roles);

        User savedUser = userRepository.save(u);

        Mentor m = new Mentor();
        m.setUser(savedUser);
        m.setDepartment(dept);
        m.setTitle(req.getTitle());

        Mentor savedMentor = mentorRepository.save(m);

        return toResponse(savedMentor);
    }

    private MentorResponse toResponse(Mentor m) {
        MentorResponse r = new MentorResponse();
        r.setId(m.getId());
        r.setTitle(m.getTitle());
        r.setCreatedAt(m.getCreatedAt());

        if (m.getUser() != null) {
            r.setUserId(m.getUser().getId());
            r.setEmail(m.getUser().getEmail());
            r.setFullName(m.getUser().getFullName());
            r.setPhone(m.getUser().getPhone());
        }

        if (m.getDepartment() != null) {
            MentorResponse.DepartmentLite d = new MentorResponse.DepartmentLite();
            d.setId(m.getDepartment().getId());
            d.setCode(m.getDepartment().getCode());
            d.setName(m.getDepartment().getName());
            r.setDepartment(d);
        }

        return r;
    }
}
