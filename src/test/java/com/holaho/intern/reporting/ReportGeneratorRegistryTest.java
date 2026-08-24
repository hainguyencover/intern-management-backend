package com.holaho.intern.reporting;

import com.holaho.intern.reporting.service.generator.ReportGenerator;
import com.holaho.intern.reporting.service.generator.ReportGeneratorRegistry;
import com.holaho.intern.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportGeneratorRegistryTest {

    private ReportGeneratorRegistry registry;
    private ReportGenerator internListGenerator;
    private ReportGenerator universityStatsGenerator;

    @BeforeEach
    void setUp() {
        internListGenerator = Mockito.mock(ReportGenerator.class);
        Mockito.when(internListGenerator.getReportCode()).thenReturn("INTERN_LIST");

        universityStatsGenerator = Mockito.mock(ReportGenerator.class);
        Mockito.when(universityStatsGenerator.getReportCode()).thenReturn("UNIVERSITY_STATISTICS");

        registry = new ReportGeneratorRegistry(List.of(internListGenerator, universityStatsGenerator));
    }

    @Test
    void shouldReturnGenerator_WhenCodeMatches() {
        ReportGenerator generator = registry.get("INTERN_LIST");
        assertNotNull(generator);
        assertEquals("INTERN_LIST", generator.getReportCode());
    }

    @Test
    void shouldThrowException_WhenCodeNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> registry.get("NON_EXISTENT_REPORT"));
    }

    @Test
    void shouldReturnAllGenerators() {
        assertEquals(2, registry.getAllGenerators().size());
    }
}
