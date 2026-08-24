package com.holaho.intern.integration.adapter;

import com.holaho.intern.integration.entity.IntegrationConnection;
import com.holaho.intern.integration.entity.IntegrationCredential;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DefaultHrmAdapter implements HrmAdapter {

    @Override
    public List<HrmEmployeeDto> fetchEmployees(IntegrationConnection connection, List<IntegrationCredential> credentials) {
        log.info("Fetching employees from HRM connection: {}, provider: {}", connection.getCode(), connection.getProvider());
        List<HrmEmployeeDto> employees = new ArrayList<>();
        employees.add(HrmEmployeeDto.builder()
                .externalId("EMP-2026-001")
                .fullName("Nguyen Van HRM")
                .email("hrm.emp001@example.com")
                .phone("0988111222")
                .departmentCode("DEV")
                .position("Software Intern")
                .dateOfBirth(LocalDate.of(2002, 5, 15))
                .status("ACTIVE")
                .build());
        employees.add(HrmEmployeeDto.builder()
                .externalId("EMP-2026-002")
                .fullName("Tran Thi HRM")
                .email("hrm.emp002@example.com")
                .phone("0988333444")
                .departmentCode("QA")
                .position("QA Intern")
                .dateOfBirth(LocalDate.of(2003, 8, 20))
                .status("ACTIVE")
                .build());
        return employees;
    }

    @Override
    public boolean testConnection(IntegrationConnection connection, List<IntegrationCredential> credentials) {
        log.info("Testing connection to HRM server baseUrl: {}", connection.getBaseUrl());
        return connection.getBaseUrl() != null && !connection.getBaseUrl().isEmpty();
    }
}
