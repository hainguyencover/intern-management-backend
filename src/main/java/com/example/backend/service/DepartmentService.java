package com.example.backend.service;

import com.example.backend.dto.request.DepartmentRequest;
import com.example.backend.entity.Department;
import com.example.backend.repository.DepartmentRepository;
import com.example.backend.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;

    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Transactional
    public Department create(DepartmentRequest req) {
        if (departmentRepository.existsByCode(req.getCode())) {
            throw new com.example.backend.exception.BadRequestException("Department code already exists: " + req.getCode());
        }
        Department d = new Department();
        d.setCode(req.getCode());
        d.setName(req.getName());
        d.setDescription(req.getDescription());
        return departmentRepository.save(d);
    }

    @Transactional
    public Department update(Long id, com.example.backend.dto.request.DepartmentRequest req) {
        Department d = departmentRepository.findById(id)
                .orElseThrow(() -> new com.example.backend.exception.NotFoundException("Department not found: " + id));

        if (!d.getCode().equals(req.getCode()) && departmentRepository.existsByCode(req.getCode())) {
            throw new com.example.backend.exception.BadRequestException("Department code already exists: " + req.getCode());
        }

        d.setCode(req.getCode());
        d.setName(req.getName());
        d.setDescription(req.getDescription());
        return departmentRepository.save(d);
    }

    @Transactional
    public void delete(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new com.example.backend.exception.NotFoundException("Department not found: " + id);
        }
        // Check dependencies
        if (programRepository.existsByDepartment_Id(id)) {
            throw new com.example.backend.exception.BadRequestException("Cannot delete department with existing programs");
        }
        departmentRepository.deleteById(id);
    }
}
