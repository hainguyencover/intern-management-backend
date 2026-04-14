/*
package com.holaho.intern.service.impl;

import com.holaho.intern.entity.Intern;
import com.holaho.intern.repository.InternRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InternServiceImplTest {

    @Mock
    private InternRepository internRepository;

    @InjectMocks
    private InternServiceImpl internService;

    private Intern intern;

    @BeforeEach
    void setUp() {
        intern = new Intern();
        intern.setId(1L);
        intern.setInternId("INT001");
        intern.setStudentId("STU001");
        intern.setFullName("Test Intern");
    }

    @Test
    void testGetInternById_Success() {
        when(internRepository.findById(anyLong())).thenReturn(Optional.of(intern));
    }
}
*/
// This file is redundant and replaced by InternProfileServiceImplTest.java

