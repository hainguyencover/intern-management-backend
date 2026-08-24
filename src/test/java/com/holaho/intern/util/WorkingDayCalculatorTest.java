package com.holaho.intern.util;

import com.holaho.intern.shared.util.WorkingDayCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WorkingDayCalculatorTest {

    @Test
    @DisplayName("Should add 3 working days when starting on a Monday")
    void testCalculateDueDateFromMonday() {
        // Monday 2026-08-24 09:00 -> Thursday 2026-08-27 09:00
        LocalDateTime start = LocalDateTime.of(2026, 8, 24, 9, 0);
        LocalDateTime due = WorkingDayCalculator.calculateDueDate(start, 3);

        assertEquals(LocalDateTime.of(2026, 8, 27, 9, 0), due);
    }

    @Test
    @DisplayName("Should skip Saturday and Sunday when starting on a Friday")
    void testCalculateDueDateFromFriday() {
        // Friday 2026-08-21 09:00 -> Wednesday 2026-08-26 09:00 (Sat & Sun skipped)
        LocalDateTime start = LocalDateTime.of(2026, 8, 21, 9, 0);
        LocalDateTime due = WorkingDayCalculator.calculateDueDate(start, 3);

        assertEquals(LocalDateTime.of(2026, 8, 26, 9, 0), due);
    }

    @Test
    @DisplayName("Should return null if start time is null")
    void testCalculateDueDateNull() {
        assertNull(WorkingDayCalculator.calculateDueDate(null, 3));
    }
}
