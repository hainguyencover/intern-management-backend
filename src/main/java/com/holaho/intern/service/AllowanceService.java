package com.holaho.intern.service;

import com.holaho.intern.entity.Allowance;
import com.holaho.intern.repository.AllowanceRepository;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.NotFoundException;


import com.holaho.intern.shared.enums.AllowanceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AllowanceService {

    private final AllowanceRepository allowanceRepository;
    private final InternProfileRepository internProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public Allowance createAllowance(Long internId, BigDecimal amount,
            LocalDate month, String notes) {
        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new NotFoundException("Intern not found"));

        Allowance allowance = new Allowance();
        allowance.setIntern(intern);
        allowance.setAmount(amount);
        allowance.setAllowanceMonth(month);
        allowance.setNotes(notes);
        allowance.setStatus(AllowanceStatus.PENDING);

        return allowanceRepository.save(allowance);
    }

    @Transactional
    public Allowance markAsPaid(Long allowanceId, Long paidById) {
        Allowance allowance = allowanceRepository.findById(allowanceId)
                .orElseThrow(() -> new NotFoundException("Allowance not found"));

        if (allowance.getStatus() != AllowanceStatus.PENDING) {
            throw new BadRequestException("Allowance is not in pending status");
        }

        User paidBy = userRepository.findById(paidById)
                .orElseThrow(() -> new NotFoundException("User not found"));

        allowance.setStatus(AllowanceStatus.PAID);
        allowance.setPaymentDate(LocalDate.now());
        allowance.setPaidBy(paidBy);

        return allowanceRepository.save(allowance);
    }

    @Transactional(readOnly = true)
    public Page<Allowance> getInternAllowances(Long internId, Pageable pageable) {
        return allowanceRepository.findByIntern_Id(internId, pageable);
    }

    @Transactional
    public Allowance updateAllowance(Long allowanceId, BigDecimal amount, String notes) {
        Allowance allowance = allowanceRepository.findById(allowanceId)
                .orElseThrow(() -> new NotFoundException("Allowance not found"));

        if (allowance.getStatus() != AllowanceStatus.PENDING) {
            throw new BadRequestException("Only pending allowances can be updated");
        }

        allowance.setAmount(amount);
        allowance.setNotes(notes);

        return allowanceRepository.save(allowance);
    }

    @Transactional(readOnly = true)
    public Page<Allowance> searchAllowances(Long internId, AllowanceStatus status,
            LocalDate monthFrom, LocalDate monthTo,
            Pageable pageable) {
        return allowanceRepository.search(internId, status, monthFrom, monthTo, pageable);
    }
}

