package com.example.backend.config;

import com.example.backend.entity.Role;
import com.example.backend.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        String[] baseRoles = {"ADMIN", "HR", "MENTOR", "INTERN", "USER"};
        for (String r : baseRoles) {
            roleRepository.findByName(r).orElseGet(() -> roleRepository.save(Role.builder().name(r).description(r + " role").build()));
        }
    }
}
